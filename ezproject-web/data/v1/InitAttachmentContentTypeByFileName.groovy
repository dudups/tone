/**
 * 20201224:
 * 上传附件保存文件类型信息，方便前端提前判断处理；
 * 故mysql添加字段；
 * 此脚本用于初始化历史数据；
 */
import com.ezone.ezproject.dal.entity.AttachmentExample
import com.ezone.ezproject.dal.mapper.AttachmentMapper
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory
import com.github.pagehelper.PageHelper
import org.apache.commons.collections.CollectionUtils
import org.apache.commons.lang3.StringUtils

def attachmentMapper = SpringBeanFactory.getBean(AttachmentMapper.class)

String parseContentType(String fileName) {
    if (StringUtils.isEmpty(fileName)) {
        return StringUtils.EMPTY;
    }
    return StringUtils.defaultString(URLConnection.guessContentTypeFromName(fileName))
}

def pageNum = 1
def pageSize = 10000
def example = new AttachmentExample()
while (true) {
    PageHelper.startPage(pageNum: pageNum, pageSize: pageSize)
    def attachments = attachmentMapper.selectByExample(example)
    if (CollectionUtils.isEmpty(attachments)) {
        break
    }
    attachments.stream()
            .filter { StringUtils.isEmpty(it.contentType) }
            .forEach {
                it.setContentType(parseContentType(it.fileName))
                attachmentMapper.updateByPrimaryKey(it)
            }
    pageNum++
}
println "done"