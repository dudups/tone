[
    [{
        "tag": "text",
        "text": "卡片$!StringEscapeUtils.escapeJava($!projectKeyCardNum)"
    }, {
       "tag": "a",
       "text": "$!StringEscapeUtils.escapeJava($!cardTitle)",
       "href": "$!cardUrl"
   }, {
        "tag": "text",
        "text": "触发了【$!StringEscapeUtils.escapeJava($!alarmName)】预警规则"
    }],
    [{
        "tag": "text",
        "text": "规则内容："
    }],
    [{
        "tag": "text",
         "text": "  字段类型：卡片字段"
    }],
    [{
        "tag": "text",
         "text": "  字段名称：$!StringEscapeUtils.escapeJava($!alarmField)"
    }],
    [{
        "tag": "text",
         "text": "  提醒规则：$!StringEscapeUtils.escapeJava($!alarmDateRule)"
    }]
]