## 删除计划时通知内容模板
[
    [{
        "tag": "text",
        "text": "$!userNickOrName在项目[$!projectName]中[$operationType.getCnName()]了计划"
    }]
    #foreach ( $deletedName in $deletedPlanNames )
    ,
    [{
        "tag": "text",
        "text": "$!deletedName"
    }]
    #end
]