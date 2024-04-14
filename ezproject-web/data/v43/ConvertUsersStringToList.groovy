/**
 * 背景: 详见data/v43
 * 问题：at_users, watch_users字段valueType定义为String了，导入场景写ES为String, 刷历史数据改为List；
 */

import com.ezone.ezproject.es.util.EsIndexUtil
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory
import org.apache.commons.lang.exception.ExceptionUtils
import org.apache.http.client.config.RequestConfig
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.index.reindex.UpdateByQueryRequest
import org.elasticsearch.script.Script
import org.elasticsearch.script.ScriptType

// 'elasticsearchRestHighLevelClient'
def es = SpringBeanFactory.getBeansOfType(RestHighLevelClient.class)[0]

def convert = {
    def requestConfig = RequestConfig.custom().setSocketTimeout(60000).build()
    def options = RequestOptions.DEFAULT.toBuilder().setRequestConfig(requestConfig).build()
    // 历史数据 at_users, watch_users
    def request = new UpdateByQueryRequest(EsIndexUtil.indexForCard())
            .setScript(new Script(
                    ScriptType.INLINE,
                    Script.DEFAULT_SCRIPT_LANG,
                    """ 
                        if (ctx._source['at_users'] instanceof String) {
                            ctx._source['at_users'] = [ctx._source['at_users']]
                        }
                        if (ctx._source['watch_users'] instanceof String) {
                            ctx._source['watch_users'] = [ctx._source['watch_users']]
                        }
                     """,
                    [:]))
    def response = es.updateByQuery(request, options)
    println(response.total)

    def eventRequest = new UpdateByQueryRequest(EsIndexUtil.indexForCardEvent())
            .setScript(new Script(
                    ScriptType.INLINE,
                    Script.DEFAULT_SCRIPT_LANG,
                    """ 
                        if (ctx._source['cardDetail.at_users'] instanceof String) {
                            ctx._source['cardDetail.at_users'] = [ctx._source['cardDetail.at_users']]
                        }
                        if (ctx._source['cardDetail.watch_users'] instanceof String) {
                            ctx._source['cardDetail.watch_users'] = [ctx._source['cardDetail.watch_users']]
                        }
                     """,
                    [:]))
    def eventResponse = es.updateByQuery(eventRequest, options)
    println(eventResponse.total)
}

try {
    convert()
} catch (Exception e) {
    println ExceptionUtils.getFullStackTrace(e)
}
println "done"
