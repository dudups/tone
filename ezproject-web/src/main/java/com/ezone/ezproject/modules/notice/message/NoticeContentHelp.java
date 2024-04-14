package com.ezone.ezproject.modules.notice.message;

import com.ezone.ezproject.common.template.VelocityTemplate;
import com.ezone.ezproject.modules.notice.bean.NoticeType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@Slf4j
@AllArgsConstructor
public class NoticeContentHelp {

    public String requestBodyForMsgPlatform(NoticeType noticeType, MessageModel model) {
        try {
            switch (noticeType) {
                case FEI_SHU:
                    return VelocityTemplate.render("model", model, String.format("/vm/notice/platform/%s.json", noticeType));
                case EMAIL:
                    return model.getEmailContent();
                case SYSTEM:
                default:
                    return model.getContent();
            }
        } catch (Exception e) {
            log.error("Render msg exception!", e);
            return e.getMessage();
        }
    }
}
