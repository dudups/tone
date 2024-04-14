package com.ezone.ezproject.modules.company.service;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.dao.CompanyCardSchemaDao;
import com.ezone.ezproject.es.dao.CompanyProjectRoleSchemaDao;
import com.ezone.ezproject.es.dao.CompanyProjectSchemaDao;
import com.ezone.ezproject.es.dao.CompanyWorkloadSettingDao;
import com.ezone.ezproject.es.entity.CompanyCardSchema;
import com.ezone.ezproject.es.entity.CompanyProjectSchema;
import com.ezone.ezproject.es.entity.CompanyWorkloadSetting;
import com.ezone.ezproject.es.entity.ProjectField;
import com.ezone.ezproject.es.entity.ProjectRole;
import com.ezone.ezproject.es.entity.ProjectRoleSchema;
import com.ezone.ezproject.es.entity.enums.RoleSource;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.company.rank.RankLocation;
import com.ezone.ezproject.modules.permission.UserProjectPermissionsService;
import com.ezone.ezproject.modules.project.bean.CompanyCardTypeConf;
import com.ezone.ezproject.modules.project.service.ProjectRoleSchemaSettingHelper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.klock.lock.Lock;
import org.springframework.boot.autoconfigure.klock.lock.LockFactory;
import org.springframework.boot.autoconfigure.klock.model.LockInfo;
import org.springframework.boot.autoconfigure.klock.model.LockType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

@Service
@Slf4j
@AllArgsConstructor
public class CompanyProjectSchemaCmdService {
    private CompanyProjectSchemaDao companyProjectSchemaDao;
    private CompanyCardSchemaDao companyCardSchemaDao;
    private CompanyProjectRoleSchemaDao companyProjectRoleSchemaDao;
    private CompanyProjectSchemaQueryService companyProjectSchemaQueryService;
    private UserProjectPermissionsService userProjectPermissionsService;

    private CompanyProjectSchemaQueryService schemaQueryService;
    private CompanyWorkloadSettingDao companyWorkloadSettingDao;

    private LockFactory lockFactory;

    private CompanyProjectSchemaSettingHelper schemaSettingHelper;
    private CompanyCardSchemaSettingHelper cardSchemaSettingHelper;
    private ProjectRoleSchemaSettingHelper roleSchemaSettingHelper;

    private RoleRankCmdService roleRankCmdService;

    private UserService userService;

    public void delete(Long companyId) throws IOException {
        companyProjectSchemaDao.delete(companyId);
    }

    public void setFields(Long companyId, List<ProjectField> fields) {
        setSchema(companyId, schema -> schemaSettingHelper.setFields(schema, schema.mergeFields(fields)));
    }

    public void setCardTypes(Long companyId, List<CompanyCardTypeConf> types) {
        setCardSchema(companyId, schema -> cardSchemaSettingHelper.setTypes(schema, types));
    }

    public void addRole(Long id, ProjectRole role) throws IOException {
        role.setKey(null);
        setRoleSchema(id, schema -> {
            String nextRank = roleRankCmdService.nextRank(schema);
            role.setRank(nextRank);
            return roleSchemaSettingHelper.addRole(schema, RoleSource.COMPANY, role);
        });
    }

    public void updateRole(Long id, ProjectRole role) {
        setRoleSchema(id, schema -> roleSchemaSettingHelper.updateRole(schema, RoleSource.COMPANY, role));
    }

    public void deleteRole(Long id, String roleKey) throws IOException {
        setRoleSchema(id, schema -> {
            // 需求暂定不强制删除，因为无需projectMemberCmdService.deleteByCompanyRole
//            projectMemberCmdService.deleteByCompanyRole(id, RoleSource.COMPANY, roleKey);
            return roleSchemaSettingHelper.deleteRole(schema, RoleSource.COMPANY, roleKey);
        });
    }

    private void setSchema(Long companyId, Function<CompanyProjectSchema, CompanyProjectSchema> set) {
        Lock lock = lockSchema(companyId);
        if (lock.acquire()) {
            try {
                CompanyProjectSchema schema = schemaQueryService.getCompanyProjectSchema(companyId);
                set.apply(schema);
                companyProjectSchemaDao.saveOrUpdate(companyId, schema);
            } catch (CodedException e) {
                throw e;
            } catch (Exception e) {
                log.error(String.format("Update project schema for company:[%s] exception!", companyId), e);
                throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            } finally {
                lock.release();
            }
        } else {
            throw new CodedException(HttpStatus.CONFLICT, "Lock for company project schema update fail!");
        }
    }

    private Lock lockSchema(Long companyId) {
        return lockFactory.getLock(new LockInfo(LockType.Reentrant,
                String.format("lock:company:project-schema:setting:%s", companyId), 2, 2));
    }

    private void setCardSchema(Long companyId, Function<CompanyCardSchema, CompanyCardSchema> set) {
        Lock lock = lockCardSchema(companyId);
        if (lock.acquire()) {
            try {
                CompanyCardSchema schema = schemaQueryService.getCompanyCardSchema(companyId);
                set.apply(schema);
                companyCardSchemaDao.saveOrUpdate(companyId, schema);
            } catch (CodedException e) {
                throw e;
            } catch (Exception e) {
                log.error(String.format("Update card schema for company:[%s] exception!", companyId), e);
                throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            } finally {
                lock.release();
            }
        } else {
            throw new CodedException(HttpStatus.CONFLICT, "Lock for company card schema update fail!");
        }
    }

    private Lock lockCardSchema(Long companyId) {
        return lockFactory.getLock(new LockInfo(LockType.Reentrant,
                String.format("lock:company:card-schema:setting:%s", companyId), 2, 2));
    }

    private void setRoleSchema(Long companyId, Function<ProjectRoleSchema, ProjectRoleSchema> set) {
        Lock lock = lockRoleSchema(companyId);
        if (lock.acquire()) {
            try {
                ProjectRoleSchema schema = companyProjectSchemaQueryService.getCompanyProjectRoleSchema(companyId);
                set.apply(schema);
                companyProjectRoleSchemaDao.saveOrUpdate(companyId, schema);
                String user = userService.currentUserName();
                userProjectPermissionsService.cacheEvict(companyId, user);
            } catch (CodedException e) {
                throw e;
            } catch (Exception e) {
                log.error(String.format("Update role schema for project:[%s] exception!", companyId), e);
                throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            } finally {
                lock.release();
            }
        } else {
            throw new CodedException(HttpStatus.CONFLICT, "Lock for project role schema update fail!");
        }
    }

    public void saveWorkloadSetting(Long companyId, CompanyWorkloadSetting workloadSetting) {
        Lock lock = lockWorkloadSetting(companyId);
        if (lock.acquire()) {
            try {
                companyWorkloadSettingDao.saveOrUpdate(companyId, workloadSetting);
            } catch (CodedException e) {
                throw e;
            } catch (Exception e) {
                log.error(String.format("Update company workload setting for company:[%s] exception!", companyId), e);
                throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            } finally {
                lock.release();
            }
        } else {
            throw new CodedException(HttpStatus.CONFLICT, "Lock for company workload setting update fail!");
        }
    }

    private Lock lockRoleSchema(Long projectId) {
        return lockFactory.getLock(new LockInfo(LockType.Reentrant,
                String.format("lock:company:project-role-setting:%s", projectId), 2, 2));
    }

    private Lock lockWorkloadSetting(Long projectId) {
        return lockFactory.getLock(new LockInfo(LockType.Reentrant,
                String.format("lock:company:project-workload-setting:%s", projectId), 2, 2));
    }

    public void rankByProjectRoleRank(Long companyId, String roleKey, String referenceRoleKey, RankLocation location) {
        setRoleSchema(companyId, schema -> (ProjectRoleSchema) roleRankCmdService.roleRank(schema, roleKey, referenceRoleKey, location, RoleSource.COMPANY));
    }
}
