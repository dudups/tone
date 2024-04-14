package com.ezone.ezproject.modules.notice.message;

import com.ezone.ezproject.common.template.VelocityTemplate;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CompanyCardSchema;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.ProjectNoticeConfig;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.card.service.CardHelper;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 单个卡片人员字段变更通知
 */
@SuperBuilder
@Getter
@Slf4j
public class CardMemberFieldChangeMessageModel extends BaseMessageModel {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
    private Map<String, Object> cardDetail;
    private List<String> changedMemberFieldKeys;
    //成员字段的用户名及昵你
    private Map<String, String> cardUserNickNames;
    //delete add
    private MemberOperatedType memberOperatedType;
    private ProjectNoticeConfig.Type cardOperatedType;
    private CompanyCardSchema companyCardSchema;
    private ProjectCardSchema projectCardSchema;

    @Override
    public String getFeiShuContent() {
        return render("/vm/notice/card/feiShu/memberFieldChangeContent.tpl");
    }

    @Override
    public String getEscapeTitle() {
        return String.format("%s %s了卡片：[%s-%s]", nickName, cardOperatedType.getCnName(), project.getKey(), FieldUtil.getSeqNum(cardDetail));
    }

    @Override
    public String getContent() {
        List<String> fieldNames = projectCardSchema.fieldNames(changedMemberFieldKeys);
        String cardPathHtml = CardHelper.cardPathHtml(project, cardDetail, endpointHelper);
        if (memberOperatedType.equals(MemberOperatedType.add)) {
            String addFormat = "<User>%s</User>在卡片%s中将你添加为%s";
            return String.format(addFormat, sender, cardPathHtml, StringUtils.join(fieldNames, "、"));
        } else {
            String removeFormat = "<User>%s</User>在卡片%s中将你从%s中移除";
            return String.format(removeFormat, sender, cardPathHtml, StringUtils.join(fieldNames, "、"));
        }
    }

    @Override
    public String getEmailContent() {
        return render("/vm/notice/card/mail/memberFieldChangeContent.html");
    }

    private String render(String resource) {
        try {
            return VelocityTemplate.render(context(), resource);
        } catch (Exception e) {
            log.error("Render msg exception!", e);
            return e.getMessage();
        }
    }

    private VelocityContext context() {
        Map<String, String> changedMemberFieldKeyNames = projectCardSchema.fieldKeyNames(changedMemberFieldKeys);
        Map<String, String> changedMemberFieldKeyValues = memberFieldValues(changedMemberFieldKeys, projectCardSchema, cardUserNickNames);
        VelocityContext context = new VelocityContext();
        context.put("StringEscapeUtils", StringEscapeUtils.class);
        context.put("userNickOrName", nickName);
        context.put("memberOperatedType", memberOperatedType);
        context.put("cardOperatedType", cardOperatedType);
        context.put("projectKey", project.getKey());
        context.put("seqNum", FieldUtil.getSeqNum(cardDetail));
        context.put("cardUrl", carHomeUrl());
        context.put("ownerUserNickNames", getOwnerUserNicknames(cardDetail, cardUserNickNames));
        context.put("userUrl", endpointHelper.userHomeUrl(FieldUtil.getCompanyId(cardDetail), sender));
        context.put("changedMemberFieldKeyNames", changedMemberFieldKeyNames);
        context.put("changedMemberFieldKeyValues", changedMemberFieldKeyValues);
        context.put("projectName", project.getName());
        context.put("cardTypeName", companyCardSchema.findCardTypeName(FieldUtil.getType(cardDetail)));
        context.put("cardCreateTime", DATE_TIME_FORMATTER.print(FieldUtil.getCreateTime(cardDetail)));
        CardField cardField = projectCardSchema.findCardField(CardField.PRIORITY);
        context.put("cardPriority", FieldUtil.getValidNameInOptions(FieldUtil.getPriority(cardDetail), cardField));
        context.put("FieldUtil", FieldUtil.class);
        context.put("cardDetail", cardDetail);
        return context;
    }

    /**
     * @param memberFieldKeys
     * @param schema
     * @param nameNickNameMap
     * @return Map key:字段key, value:用户名对应的昵称
     */
    private Map<String, String> memberFieldValues(List<String> memberFieldKeys, ProjectCardSchema schema, Map<String, String> nameNickNameMap) {
        Map<String, String> fieldKeyUserNickNamesMap = new HashMap<>();
        if (CollectionUtils.isEmpty(memberFieldKeys)) {
            return fieldKeyUserNickNamesMap;
        }
        for (CardField field : schema.getFields()) {
            if (!memberFieldKeys.contains(field.getKey())) {
                continue;
            }
            Object value = cardDetail.get(field.getKey());
            if (null == value) {
                continue;
            }
            switch (field.getType()) {
                case MEMBER:
                case USER:
                    fieldKeyUserNickNamesMap.put(field.getKey(), nameNickNameMap.get(FieldUtil.toString(value)));
                    break;
                case MEMBERS:
                case USERS:
                    List<String> usernames = FieldUtil.toStringList(value);
                    List<String> nicknames = new ArrayList<>();
                    usernames.forEach(username -> nicknames.add(nameNickNameMap.get(username)));
                    fieldKeyUserNickNamesMap.put(field.getKey(), StringUtils.join(nicknames, "，"));
            }
        }
        return fieldKeyUserNickNamesMap;
    }

    private String carHomeUrl() {
        return endpointHelper.cardDetailUrl(FieldUtil.getCompanyId(cardDetail), project.getKey(), FieldUtil.getSeqNum(cardDetail));
    }

    public enum MemberOperatedType {
        add("添加"), delete("删除");

        private final String cnName;

        MemberOperatedType(String cnName) {
            this.cnName = cnName;
        }

        /**
         * 获取操作名称（tpl中有使用）
         *
         * @return 操作名称
         */
        public String getCnName() {
            return cnName;
        }
    }
}
