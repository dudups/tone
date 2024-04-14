// json example:
// var json = {
//     "aggregations": {
//         "date_histogram#X": {
//             "buckets": [{
//                 "key_as_string": "2020-08-06",
//                 "key": 1596672000000,
//                 "doc_count": 4,
//                 "sterms#Y": {
//                     "doc_count_error_upper_bound": 0,
//                     "sum_other_doc_count": 0,
//                     "buckets": [{
//                         "key": "story",
//                         "doc_count": 3,
//                         "value_count#Y": {
//                             "value": 3
//                         }
//                     }, {
//                         "key": "bug",
//                         "doc_count": 1,
//                         "value_count#Y": {
//                             "value": 1
//                         }
//                     }]
//                 }
//             }, {
//                 "key_as_string": "2020-08-07",
//                 "key": 1596758400000,
//                 "doc_count": 33,
//                 "sterms#Y": {
//                     "doc_count_error_upper_bound": 0,
//                     "sum_other_doc_count": 0,
//                     "buckets": [{
//                         "key": "story",
//                         "doc_count": 33,
//                         "value_count#Y": {
//                             "value": 33
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
    var yValues = [];
    var getKv = function(buckets) {
        var result = [];
        for each (var bucket in buckets) {
            for (var key in bucket) {
                /#/.test(key) && result.push({
                    key: bucket.key,
                    value: bucket[key].value
                });
            }
            yValues.push(bucket.key);
        }
        return result;
    }
    var aggregations = JSON.parse(json).aggregations;
    var resultX = [];
    for each (var bucket in aggregations['date_histogram#X'].buckets) {
        resultX.push({
            key: bucket['key'],
            key_as_string: bucket['key_as_string'],
            y: getKv(getV(bucket, /#Y/).buckets)
        });
    }
    return JSON.stringify({
        data: resultX,
        yValues: yValues
    });
}