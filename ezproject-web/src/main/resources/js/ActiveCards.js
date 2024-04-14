// {
//     "aggregations": {
//     "sterms#X": {
//         "doc_count_error_upper_bound": 0,
//             "sum_other_doc_count": 0,
//             "buckets": [
//             {
//                 "key": "yinchengfeng_dev",
//                 "doc_count": 322,
//                 "sum#estimate_workload": {
//                     "value": 0.0
//                 },
//                 "sterms#Y": {
//                     "doc_count_error_upper_bound": 0,
//                     "sum_other_doc_count": 0,
//                     "buckets": [
//                         {
//                             "key": "bug",
//                             "doc_count": 321
//                         },
//                         {
//                             "key": "story",
//                             "doc_count": 1
//                         }
//                     ]
//                 }
//             },
//             {
//                 "key": "yinchengfeng110",
//                 "doc_count": 1,
//                 "sum#estimate_workload": {
//                     "value": 0.0
//                 },
//                 "sterms#Y": {
//                     "doc_count_error_upper_bound": 0,
//                     "sum_other_doc_count": 0,
//                     "buckets": [
//                         {
//                             "key": "bug",
//                             "doc_count": 1
//                         }
//                     ]
//                 }
//             }
//         ]
//     },
//     "value_count#count": {
//         "value": 335
//     }
// }
// }
function render(json) {
    var getV = function(obj, keyTest) {
        for (var key in obj) {
            if (keyTest.test(key)) return obj[key];
        }
        return undefined;
    }
    var getKv = function(buckets) {
        var result = [];
        for each (var bucket in buckets) {
            result.push({
                key: bucket.key,
                count: bucket.doc_count
            });
        }
        return result;
    }

    var aggregations = JSON.parse(json).aggregations;
    var resultX = [];
    var xValues = [];
    for each (var bucket in getV(aggregations, /#X/).buckets) {
        resultX.push({
            key: bucket.key,
            count: bucket.doc_count,
            sum: bucket["sum#estimate_workload"].value,//sum 用户总的估计工时
            y: getKv(getV(bucket,/#Y/).buckets)
        });
        xValues.push(bucket.key);
    }
    return JSON.stringify({
        x: resultX,
        total: aggregations['value_count#count'].value,
        xValues: xValues
    });
}