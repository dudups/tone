#foreach( $event in $events )
[$dateFormat.apply($event.date)][$event.user]: ##
    #if ($event.eventType == "CREATE")
创建了卡片;##
    #elseif ($event.eventType == "UPDATE")
        #foreach( $fieldMsg in $event.eventMsg.fieldMsgs )
更新了字段[$fieldMsg.fieldMsg];##
        #end
        #foreach( $fieldDetailMsg in $event.eventMsg.fieldDetailMsgs )
更新了字段[$fieldDetailMsg.fieldMsg], 值由[$!fieldFormat.format($fieldDetailMsg.fieldKey,$fieldDetailMsg.fromMsg)]改为[$!fieldFormat.format($fieldDetailMsg.fieldKey,$fieldDetailMsg.toMsg)];##
        #end
    #elseif ($event.eventType == "DELETE")
删除了卡片;##
    #elseif ($event.eventType == "RECOVERY")
还原了卡片;##
    #elseif ($event.eventType == "ADD_ATTACHMENT")
添加了卡片附件[$event.eventMsg.fileName];##
    #elseif ($event.eventType == "RM_ATTACHMENT")
删除了卡片附件[$event.eventMsg.fileName];##
    #elseif ($event.eventType == "ADD_RELATE_CARD")
关联了卡片[$event.eventMsg.cardKey];##
    #elseif ($event.eventType == "RM_RELATE_CARD")
解除关联卡片[$event.eventMsg.cardKey];##
    #elseif ($event.eventType == "AUTO_STATUS_FLOW")
[$event.eventMsg.eventType.description]卡片流转事件导致状态自动从[$event.eventMsg.fromMsg]流转到[$event.eventMsg.toMsg];##
    #else
$event.eventType事件;##
    #end
#end
