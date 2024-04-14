// json example:
// var json =
//     {
//         "aggregations": {
//             "sterms#X": {
//                 "doc_count_error_upper_bound": 0,
//                 "sum_other_doc_count": 0,
//                 "buckets": [
//                     {
//                         "key": "task",
//                         "doc_count": 8,
//                         "filter#Y": {
//                             "doc_count": 1
//                         }
//                     },
//                     {
//                         "key": "bug",
//                         "doc_count": 4,
//                         "filter#Y": {
//                             "doc_count": 0
//                         }
//                     },
//                     {
//                         "key": "story",
//                         "doc_count": 1,
//                         "filter#Y": {
//                             "doc_count": 0
//                         }
//                     }
//                 ]
//             }
//         }
//     }
function render(json) {
    var aggregations = JSON.parse(json).aggregations;
    var result = {};
    for each (var agg in aggregations) {
        for each (var bucket in agg.buckets) {
            var bucketValues={}
            bucketValues["total"] = bucket.doc_count
            bucketValues["end"] = bucket["filter#Y"].doc_count
            result[bucket.key] = bucketValues
            // result[bucket.key][1] = bucket.filter.doc_count
        }
    }
    return JSON.stringify(result);
}