## 卡片成员字段变化通知
[
#if( $memberOperatedType == "add")
        [{
            "tag": "text",
            "text": "项目名称：$!StringEscapeUtils.escapeJava($!projectName)"
        }],
        [{
            "tag": "text",
            "text": "卡片标题："
        }, {
            "tag": "a",
            "text": "$!StringEscapeUtils.escapeJava($!FieldUtil.getTitle($!cardDetail))",
            "href": "$!cardUrl"
        }],
        [{
            "tag": "text",
            "text": "卡片类型：$cardTypeName"
        }],
        [{
            "tag": "text",
            "text": "创建时间：$cardCreateTime"
        }],
        [{
            "tag": "text",
            "text": "负责人：$!StringEscapeUtils.escapeJava($!ownerUserNickNames)"
        }]
        #foreach( $field in $changedMemberFieldKeyNames.entrySet() )
            #if( $field.key != "owner_users" )
        ,[{
            "tag": "text",
            "text": "$!field.value：$!StringEscapeUtils.escapeJava($!changedMemberFieldKeyValues.get($!field.key))"
        }]
            #end
        #end
        #if( $!FieldUtil.getPriority($!cardDetail) )
        ,[{
            "tag": "text",
            "text": "优先级：$!cardPriority"
        }]
        #end
#elseif( $memberOperatedType == "delete" )
    #set( $fieldNames = $changedMemberFieldKeyNames.values())
         [{
             "tag": "text",
             "text": "$!StringEscapeUtils.escapeJava($!userNickOrName)"
         }, {
             "tag": "text",
             "text": "在卡片 "
         }, {
             "tag": "a",
             "text": "$!projectKey-$!seqNum:$!StringEscapeUtils.escapeJava($FieldUtil.getTitle($!cardDetail))",
             "href": "$!cardUrl"
         }, {
             "tag": "text",
             "text": "中将你从$!fieldNames中移除"
         }]
#end
]