## 添加或更新计划时通知内容模板
[
    [{
        "tag": "text",
        "text": "$!userNickOrName在项目[$!projectName]中[$operationType.getCnName()]了计划"
    }],
    [{
        "tag": "a",
        "text": "[$!planName]",
        "href": "$!planUrl"
    }]
]