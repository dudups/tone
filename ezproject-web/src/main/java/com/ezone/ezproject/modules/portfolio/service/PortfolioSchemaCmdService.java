package com.ezone.ezproject.modules.portfolio.service;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.common.exception.ErrorCode;
import com.ezone.ezproject.es.dao.PortfolioRoleSchemaDao;
import com.ezone.ezproject.es.entity.PortfolioRole;
import com.ezone.ezproject.es.entity.PortfolioRoleSchema;
import com.ezone.ezproject.es.entity.enums.RoleSource;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.company.rank.RankLocation;
import com.ezone.ezproject.modules.company.service.RoleRankCmdService;
import com.ezone.ezproject.modules.portfolio.bean.DeletePortfolioRoleOptions;
import com.ezone.ezproject.modules.project.bean.RoleKeySource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.klock.lock.Lock;
import org.springframework.boot.autoconfigure.klock.lock.LockFactory;
import org.springframework.boot.autoconfigure.klock.model.LockInfo;
import org.springframework.boot.autoconfigure.klock.model.LockType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.function.Function;

@Transactional(rollbackFor = Exception.class)
@Service
@Slf4j
@AllArgsConstructor
public class PortfolioSchemaCmdService {
    private PortfolioRoleSchemaDao portfolioRoleSchemaDao;
    private PortfolioRoleSchemaSettingHelper roleSchemaSettingHelper;
    private PortfolioSchemaQueryService schemaQueryService;
    private RoleRankCmdService roleRankCmdService;
    private PortfolioMemberQueryService portfolioMemberQueryService;
    private PortfolioMemberCmdService portfolioMemberCmdService;
    private UserPortfolioPermissionsService userPortfolioPermissionsService;
    private UserService userService;

    private LockFactory lockFactory;

    public void setPortfolioRoleSchema(Long portfolioId) {
        PortfolioRoleSchema schema = roleSchemaSettingHelper.defaultRoleSchema();
        try {
            portfolioRoleSchemaDao.saveOrUpdate(portfolioId, schema);
        } catch (IOException e) {
            log.error("[setPortfolioRoleSchema][" + " portfolioId :" + portfolioId + "; schema :" + schema + "][error][" + e.getMessage() + "]", e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public void addRole(Long id, PortfolioRole role) throws IOException {
        role.setKey(null);
        setRoleSchema(id, schema -> {
            String nextRank = roleRankCmdService.nextRank(schema);
            role.setRank(nextRank);
            return roleSchemaSettingHelper.addRole(schema, RoleSource.CUSTOM, role);
        });
    }

    private void setRoleSchema(Long portfolioId, Function<PortfolioRoleSchema, PortfolioRoleSchema> set) {
        Lock lock = lockRoleSchema(portfolioId);
        if (lock.acquire()) {
            try {
                PortfolioRoleSchema schema = schemaQueryService.getPortfolioRoleSchema(portfolioId);
                set.apply(schema);
                portfolioRoleSchemaDao.saveOrUpdate(portfolioId, PortfolioRoleSchema.builder()
                        .roles(schema.getRoles())
                        .maxRank(schema.getMaxRank())
                        .build());
                String user = userService.currentUserName();
                userPortfolioPermissionsService.cacheEvict(portfolioId, user);
            } catch (CodedException e) {
                throw e;
            } catch (Exception e) {
                log.error(String.format("Update role schema for portfolio:[%s] exception!", portfolioId), e);
                throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            } finally {
                lock.release();
            }
        } else {
            throw new CodedException(HttpStatus.CONFLICT, "Lock for portfolio role schema update fail!");
        }

    }

    private Lock lockRoleSchema(Long portfolioId) {
        return lockFactory.getLock(new LockInfo(LockType.Reentrant,
                String.format("lock:portfolio:role-setting:%s", portfolioId), 2, 2));
    }

    public void deleteRole(Long id, String roleKey, DeletePortfolioRoleOptions deleteOptions) {
        PortfolioRole role = (PortfolioRole) schemaQueryService.getPortfolioRoleSchema(id).findRole(RoleSource.CUSTOM, roleKey);
        if (role == null) {
            throw CodedException.NOT_FOUND;
        }
        setRoleSchema(id, schema -> {
            RoleKeySource sourceTargetRole = RoleKeySource.builder().role(roleKey).roleSource(RoleSource.CUSTOM).build();
            boolean hasMember = portfolioMemberQueryService.hasMember(id, RoleSource.CUSTOM, roleKey);
            if (deleteOptions == null && hasMember) {
                throw new CodedException(ErrorCode.NEED_MIGRATE_ROLE_USER, "需要将此角色下的用户迁移到其他角色下");
            }
            if (deleteOptions != null && deleteOptions.isMigrate()) {
                if (hasMember && (deleteOptions.getMigrateToRole() == null || deleteOptions.getMigrateToRole().getRole() == null)) {
                    throw new CodedException(HttpStatus.BAD_REQUEST, "需指定迁移的角色信息");
                }
                if (hasMember && deleteOptions.getMigrateToRole().getRole().equals(roleKey) && deleteOptions.getMigrateToRole().getRoleSource().equals(RoleSource.CUSTOM)) {
                    throw new CodedException(HttpStatus.BAD_REQUEST, "不能迁移到自身");
                }
                //迁移角色成员，可能还有角色中相同成员未迁移
                portfolioMemberCmdService.migrateRoleUsers(id, sourceTargetRole, deleteOptions.getMigrateToRole());
            }
            //删除角色成员
            portfolioMemberCmdService.deleteByPortfolioRole(id, RoleSource.CUSTOM, roleKey);
            return roleSchemaSettingHelper.deleteRole(schema, RoleSource.CUSTOM, roleKey);
        });
    }

    public void updateRole(Long id, PortfolioRole role) {
        setRoleSchema(id, schema -> {
            roleSchemaSettingHelper.updateRole(schema, RoleSource.CUSTOM, role);
            return schema;
        });
    }

    public void customerRoleRank(Long id, String roleKey, String referenceRoleKey, RankLocation location) {
        setRoleSchema(id, schema -> (PortfolioRoleSchema) roleRankCmdService.roleRank(schema, roleKey, referenceRoleKey, location, RoleSource.CUSTOM));
    }

    public void delete(Long id) {
        try {
            portfolioRoleSchemaDao.delete(id);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
