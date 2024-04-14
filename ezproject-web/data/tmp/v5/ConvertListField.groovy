/**
 * 背景: dsw客户"九识"，通过api创建卡片，指定负责人/qa负责人，都是单String而不是String数组，前端后续对于单String可能处理错误
 * 问题：把历史的负责人等有问题的单String，刷成List
 */


import com.ezone.ezproject.common.EsUtil
import com.ezone.ezproject.es.util.EsIndexUtil
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory
import org.apache.commons.lang.exception.ExceptionUtils
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.reindex.UpdateByQueryRequest
import org.elasticsearch.script.Script
import org.elasticsearch.script.ScriptType

// 'elasticsearchRestHighLevelClient'
def es = SpringBeanFactory.getBeansOfType(RestHighLevelClient.class)[0]

def convert =  { String field ->
    def params = [:]
    def script = """
        Object value = ctx._source['$field'];
        if (!(value == null || value instanceof List)) {
            ctx._source['$field'] = [value];
        }
    """
    def request = new UpdateByQueryRequest(EsIndexUtil.indexForCard())
            .setQuery(QueryBuilders.existsQuery(field))
            .setScript(new Script(
                    ScriptType.INLINE,
                    Script.DEFAULT_SCRIPT_LANG,
                    script,
                    params))
    def response = es.updateByQuery(request, EsUtil.REQUEST_OPTIONS)
    println(response.total)
}
try {
    convert("owner_users")
    convert("qa_owner_users")
} catch (Exception e) {
    println ExceptionUtils.getFullStackTrace(e)
}
println "done"
