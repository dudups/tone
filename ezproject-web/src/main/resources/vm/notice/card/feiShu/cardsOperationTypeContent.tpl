[
    [{
        "tag": "text",
        "text": "$!StringEscapeUtils.escapeJava($!userNickOrName)"
    }, {
        "tag": "text",
        "text": "批量$!{operationType.cnName}了$!cardDetails.size()张卡片(仅显示为$size张)："
    }]
    #set( $max = $size - 1 )
     #foreach ( $count in [0..$max] )
        #set( $cardDetail = $cardDetails.get($count) )
        #if( $cardDetail )
            #set( $seqNum = $cardDetail.get("seq_num") )
            #set( $cardTitle = $cardDetail.get("title") )
    , [{
         "tag": "text",
         "text": "[$projectKey-$seqNum]"
    }, {
        "tag": "a",
        "text": "$!StringEscapeUtils.escapeJava($cardTitle)",
        "href": "$context.cardUrl($!companyId, $!projectKey, $!seqNum)"
    }]
        #end
    #end
]