import com.ezone.galaxy.framework.common.spring.SpringBeanFactory
import org.apache.commons.lang.exception.ExceptionUtils
import org.redisson.Redisson
import org.redisson.api.RedissonClient

def redisCli = SpringBeanFactory.getBean(Redisson.class) as RedissonClient
def clean = { String name ->
    try {
        redisCli.getBucket(name).delete()
    } catch (Exception e) {
        println ExceptionUtils.getFullStackTrace(e)
    }
}
clean("cache:UserProjectPermissionsService:userProjectPermissions")
clean("cache:ProjectQueryService.select")
clean("cache:ProjectSchemaService.getProjectRoleSchema")
clean("cache:ProjectSchemaService.getProjectCardSchema")
clean("cache:ProjectSchemaService.getCompanyCardSchema")
clean("cache:ProjectSchemaService.getCompanyProjectRoleSchema")
println "done"