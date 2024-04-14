// json example:
// var json = {
//     "aggregations": {
//         "sterms#type": {
//             "doc_count_error_upper_bound": 0,
//             "sum_other_doc_count": 0,
//             "buckets": [{
//                 "key": "story",
//                 "doc_count": 36
//             }, {
//                 "key": "bug",
//                 "doc_count": 1
//             }]
//         }
//     }
// }
function render(json) {
    var aggregations = JSON.parse(json).aggregations;
    var result = {};
    for each (var agg in aggregations) {
        for each (var bucket in agg.buckets) {
            result[bucket.key] = bucket.doc_count;
        }
    }
    return JSON.stringify(result);
}