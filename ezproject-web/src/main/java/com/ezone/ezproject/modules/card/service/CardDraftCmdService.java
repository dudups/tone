package com.ezone.ezproject.modules.card.service;

import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.es.dao.CardDraftDao;
import com.ezone.ezproject.es.entity.CardDraft;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.attachment.service.CardAttachmentCmdService;
import com.ezone.ezproject.modules.card.bean.query.Eq;
import com.ezone.ezproject.modules.card.bean.query.Lt;
import com.ezone.ezproject.modules.project.service.ProjectQueryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class CardDraftCmdService {
    private CardDraftDao cardDraftDao;

    private UserService userService;
    private CardAttachmentCmdService cardAttachmentCmdService;

    private ProjectQueryService projectQueryService;

    public Long create(Long projectId) throws IOException {
        Long id = IdUtil.generateId();
        cardDraftDao.saveOrUpdate(id, CardDraft.builder()
                .createTime(new Date())
                .user(userService.currentUserName())
                .projectId(projectId)
                .build());
        return id;
    }

    public void delete(Long id) throws IOException {
        CardDraft cardDraft = cardDraftDao.find(id);
        if (null == cardDraft) {
            return;
        }
        Long companyId = projectQueryService.getProjectCompany(cardDraft.getProjectId());
        cardAttachmentCmdService.delete(companyId, Arrays.asList(id));
        cardDraftDao.delete(id);
    }

    public void deleteByProject(Long projectId) throws IOException {
        List<Long> ids = cardDraftDao.searchIds(Eq.builder().field(CardDraft.PROJECT_ID).value(String.valueOf(projectId)).build());
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        Long companyId = projectQueryService.getProjectCompany(projectId);
        cardAttachmentCmdService.delete(companyId, ids);
        cardDraftDao.delete(ids);
    }

    public void onCommit(Long id) throws IOException {
        cardDraftDao.delete(id);
    }

    public void clean() throws IOException {
        List<Long> ids = cardDraftDao.searchIds(Lt.builder().field(CardDraft.CREATE_TIME).value(String.valueOf(DateUtils.addDays(new Date(), -1).getTime())).build());
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        ids.forEach(id -> {
            try {
                delete(id);
            } catch (Exception e) {
                log.error(String.format("clean draft[{}] exception!"), e);
            }
        });
    }

}
