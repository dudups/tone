## 卡片催办通知的飞书模板
[
    [{
        "tag": "text",
        "text": "项目名称：$!StringEscapeUtils.escapeJava($!projectName)"
    }],
    [{
        "tag": "text",
        "text": "卡片标题："
    }, {
        "tag": "a",
        "text": "$!StringEscapeUtils.escapeJava($!cardTitle)",
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
    #if( $cardPriority )
    ,[{
        "tag": "text",
        "text": "优先级：$!cardPriority"
    }]
    #end
    ,[{
        "tag": "text",
        "text": "催办内容：$!StringEscapeUtils.escapeJava($!remindContent)"
    }]
]