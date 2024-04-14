// json example:
// var json = {
//     "aggregations": {
//         "value_count#total": {
//             "value": 37
//         },
//         "sterms#X": {
//             "doc_count_error_upper_bound": 0,
//             "sum_other_doc_count": 0,
//             "buckets": [{
//                 "key": "story",
//                 "doc_count": 36,
//                 "value_count#X": {
//                     "value": 36
//                 },
//                 "sterms#Y": {
//                     "doc_count_error_upper_bound": 0,
//                     "sum_other_doc_count": 0,
//                     "buckets": [{
//                         "key": "open",
//                         "doc_count": 35,
//                         "value_count#Y": {
//                             "value": 35
//                         }
//                     }, {
//                         "key": "closed",
//                         "doc_count": 1,
//                         "value_count#Y": {
//                             "value": 1
//                         }
//                     }]
//                 }
//             }, {
//                 "key": "bug",
//                 "doc_count": 1,
//                 "value_count#X": {
//                     "value": 1
//                 },
//                 "sterms#Y": {
//                     "doc_count_error_upper_bound": 0,
//                     "sum_other_doc_count": 0,
//                     "buckets": [{
//                         "key": "open",
//                         "doc_count": 1,
//                         "value_count#Y": {
//                             "value": 1
//                         }
//                     }]
//                 }
//             }]
//         }
//     }
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
        for (var key in bucket) {
            /#SUM/.test(key) && resultX.push({
                key: bucket.key,
                sum: bucket[key].value,
                count: bucket.doc_count,
                y: getKv(getV(bucket,/#Y/).buckets)
            });
        }
        xValues.push(bucket.key);
    }
    return JSON.stringify({
        x: resultX,
        total: aggregations['value_count#count'].value,
        xValues: xValues
    });
}