/**
 * 背景: 详见data/v27
 * 问题：卡片完成但是延迟，添加了冗余计算字段，刷历史数据；
 */


import com.ezone.ezproject.common.EsUtil
import com.ezone.ezproject.es.util.EsIndexUtil
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory
import org.apache.commons.lang.exception.ExceptionUtils
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.index.reindex.UpdateByQueryRequest
import org.elasticsearch.script.Script
import org.elasticsearch.script.ScriptType

// 'elasticsearchRestHighLevelClient'
def es = SpringBeanFactory.getBeansOfType(RestHighLevelClient.class)[0]

def convert = {

    // 历史数据可能存的是2022-01-01开头的时间格式，从定义只允许日期，故截取并转换为时间戳数字格式
    def request = new UpdateByQueryRequest(EsIndexUtil.indexForCard())
            .setScript(new Script(
                    ScriptType.INLINE,
                    Script.DEFAULT_SCRIPT_LANG,
                    "ctx._source['end_date'] = ctx._source['end_date'] instanceof String && ctx._source['end_date'].contains('-') ? new SimpleDateFormat('yyyy-MM-dd').parse(ctx._source['end_date'].substring(0, 10)).getTime() : ctx._source['end_date'] ",
                    [:]))
    def response = es.updateByQuery(request, EsUtil.REQUEST_OPTIONS)
    println(response.total)
}

def task = {
    // 历史数据还可能是数字字符串，所以直接访问底层存储，得先parseLong才能比较(tcl)
    def request = new UpdateByQueryRequest(EsIndexUtil.indexForCard())
            .setScript(new Script(
                    ScriptType.INLINE,
                    Script.DEFAULT_SCRIPT_LANG,
                    "ctx._source['last_end_delay'] = ctx._source['calc_is_end'] && ctx._source['end_date'] != null && ctx._source['last_end_time'] != null && (ctx._source['end_date'] instanceof String ? Long.parseLong(ctx._source['end_date']) : ctx._source['end_date']) < ctx._source['last_end_time']",
                    [:]))
    def response = es.updateByQuery(request, EsUtil.REQUEST_OPTIONS)
    println(response.total)
}

try {
    convert()
    task()
} catch (Exception e) {
    println ExceptionUtils.getFullStackTrace(e)
}
println "done"
