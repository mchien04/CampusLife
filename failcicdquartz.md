



Claim offer

Đúng là về cơ bản ảnh và file đều là "file", cách lưu trong hệ thống gần như giống nhau. Khác nhau chủ yếu ở mục đích sử dụng, cách xử lý, metadata và nơi lưu path/URL.

Ví dụ với app của bạn:

Railway DB
    |
    +-- users
    |      avatar_url
    |
    +-- products
           image_url

S3
    |
    +-- images/
    |      avatar/abc.jpg
    |      product/p1.png
    |
    +-- files/
           contract.pdf
           report.xlsx
Trong DB thường chỉ lưu đường dẫn (URL/key):

Ví dụ:

users

id | name | avatar_url
-------------------------------
1  | An   | images/avatar/a.jpg
S3 giữ file thật:

bucket-name
└── images
    └── avatar
        └── a.jpg
Ảnh
Thường có:

{
  "url": "images/products/abc.jpg",
  "type": "image/jpeg",
  "size": 204800,
  "width": 800,
  "height": 600
}
Xử lý thêm:

resize thumbnail

nén

convert WebP

cache CDN

Ví dụ:

images/
 ├── original/
 │      product1.jpg
 │
 └── thumbnail/
        product1.webp
File
Ví dụ:

PDF

Word

Excel

ZIP

Thường chỉ cần:

{
  "url": "files/report.pdf",
  "filename": "report.pdf",
  "size": 2000000
}
Không resize, không tạo thumbnail (trừ preview).

Video
Cũng giống file nhưng thường thêm:

videos/
 └── course/
      video1.mp4
Metadata:

{
 "duration":3600,
 "resolution":"1080p"
}
Có thể cần:

encode

streaming

HLS (.m3u8)

Thiết kế DB tốt hơn
Không nên tạo:

users
    avatar_url

products
    image_url

documents
    file_url
Khi app lớn sẽ rối.

Nên tạo bảng chung:

media

id
file_name
file_path
file_type
mime_type
size
created_at
Ví dụ:

media

1
avatar.jpg
images/avatar/avatar.jpg
image/jpeg
200KB


2
contract.pdf
files/contracts/a.pdf
application/pdf
2MB
Rồi liên kết:

users
 |
 avatar_id
 |
 media.id
Với Spring Boot, thường tạo:

@Entity
class Media {

 Long id;

 String path;

 String originalName;

 String contentType;

 Long size;
}
Upload xong:

S3
 ↓
path trả về
 ↓
save Media
 ↓
entity khác tham chiếu
Tóm lại:

Ảnh/file/video đều lưu cùng một kiểu: object storage (S3/R2...)

DB chỉ lưu URL hoặc key

Khác nhau ở:

folder/path

metadata

cách xử lý

Ví dụ:

images/user/a.jpg
files/document/a.pdf
videos/course/a.mp4
chứ không phải có "database lưu ảnh" và "database lưu file" riêng.


cron với quartz trong spring

Trong Spring (đặc biệt Spring Boot), cron và Quartz đều dùng để chạy tác vụ định kỳ (scheduled jobs), nhưng mức độ khác nhau.

Nói ngắn gọn:

@Scheduled + cron → đơn giản, nhẹ, chạy các job cơ bản

Quartz → hệ thống scheduler mạnh hơn, có quản lý job, lưu DB, retry, cluster...

1. Spring Cron (@Scheduled)
Đây là cách phổ biến nhất.

Thêm dependency:

Maven:

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
</dependency>
Tạo job:

@Component
public class MyJob {


    @Scheduled(cron = "0 0 0 * * *")
    public void run() {

        System.out.println("Chạy mỗi ngày lúc 00:00");

    }

}
Cron này:

giây phút giờ ngày tháng thứ
Ví dụ:

Cron	Ý nghĩa
0 * * * * *	mỗi phút
0 0 * * * *	mỗi giờ
0 0 0 * * *	mỗi ngày 00:00
0 0 9 * * MON-FRI	9h sáng thứ 2-6
Bật scheduling:

@SpringBootApplication
@EnableScheduling
public class Application {

}
2. Vấn đề của @Scheduled
Ví dụ bạn deploy:

EC2
 |
Spring Boot
 |
@Scheduled job
Job chạy trong instance đó.

Nếu scale:

EC2-1
 |
job chạy

EC2-2
 |
job cũng chạy
Kết quả:

gửi email 2 lần
trừ tiền 2 lần
Không có quản lý lock.

3. Quartz là gì?
Quartz là scheduler chuyên nghiệp.

Nó có:

Job

Trigger

Schedule

Persistence

Retry

Misfire handling

Cluster support

Kiến trúc:

Spring Boot

Quartz Scheduler

      |
      |
Database
(quartz tables)

      |
      |
Jobs
4. Thêm Quartz
Maven:

<dependency>
    <groupId>
      org.springframework.boot
    </groupId>

    <artifactId>
      spring-boot-starter-quartz
    </artifactId>
</dependency>
5. Tạo Quartz Job
Class job:

@Component
public class EmailJob extends QuartzJobBean {


@Override
protected void executeInternal(
JobExecutionContext context) {


System.out.println(
"Gửi email"
);


}

}
Tạo schedule:

@Configuration
public class QuartzConfig {


@Bean
public JobDetail emailJobDetail(){


return JobBuilder
.newJob(EmailJob.class)
.withIdentity("emailJob")
.storeDurably()
.build();

}



@Bean
public Trigger emailTrigger(){


CronScheduleBuilder cron =
CronScheduleBuilder
.cronSchedule(
"0 0 9 * * ?"
);


return TriggerBuilder
.newTrigger()
.forJob(emailJobDetail())
.withSchedule(cron)
.build();


}

}
6. Khi nào dùng cái nào?
Dùng @Scheduled khi:
✔ xóa cache

mỗi 10 phút
✔ đồng bộ dữ liệu nhỏ

mỗi giờ
✔ gửi notification đơn giản

mỗi ngày
✔ app nhỏ

Dùng Quartz khi:
✔ hệ thống lớn

Ví dụ:

User đặt lịch:
- gửi email sau 3 ngày
- nhắc thanh toán ngày mai
- chạy báo cáo cuối tháng
✔ cần lưu job trong DB

Ví dụ:

restart server

job vẫn còn
✔ nhiều server

EC2-1
EC2-2
EC2-3

chỉ 1 server chạy job
Với stack của bạn (Spring Boot + EC2)
Mình sẽ chọn:

App nhỏ:
Spring Boot
 |
@Scheduled
 |
Railway DB
Ví dụ:

cleanup

sync

cron report

App production nhiều user:
Spring Boot
 |
Quartz
 |
PostgreSQL
 |
EC2 cluster
Ví dụ:

order

payment

subscription

email queue

Một lưu ý quan trọng: nếu bạn dùng EC2 + Spring Boot + Docker/Kubernetes sau này, đừng dùng cron Linux (crontab) để chạy logic nghiệp vụ. Nên để job nằm trong Spring (@Scheduled/Quartz) để quản lý cùng code.



Pasted text.txt
Document

Lỗi này là do bạn đã bật Quartz dùng JDBC JobStore, nhưng database test (H2) chưa có các bảng Quartz. Log chính:

Table "QRTZ_LOCKS" not found
Quartz đang cố chạy:

SELECT * FROM QRTZ_LOCKS
nhưng H2 test DB không có bảng:

QRTZ_LOCKS
QRTZ_JOB_DETAILS
QRTZ_TRIGGERS
...


Cách 1 (khuyên dùng): Test thì không cần Quartz
Bạn đang lỗi ở:

CampusLifeApplicationTests.contextLoads
Đây chỉ là test load Spring Context.

Tạo file:

src/test/resources/application-test.properties
Thêm:

spring.quartz.auto-startup=false
Vì log đang chạy profile:

activeProfiles = ["test"]


Khi test:

Spring Boot
 |
load context
 |
không start Quartz
 |
test pass
Cách 2: Cho Quartz tự tạo bảng
Nếu muốn test Quartz luôn:

application-test.properties

spring.quartz.job-store-type=jdbc
spring.quartz.jdbc.initialize-schema=always
Spring sẽ chạy script tạo:

QRTZ_LOCKS
QRTZ_TRIGGERS
QRTZ_JOB_DETAILS
...
Cách 3: Production dùng Railway PostgreSQL
Với môi trường thật:

application-prod.properties

spring.quartz.job-store-type=jdbc
spring.quartz.jdbc.initialize-schema=never
Sau đó chạy SQL schema Quartz một lần.

Ví dụ PostgreSQL:

org/quartz/impl/jdbcjobstore/tables_postgres.sql
Tạo bảng trong Railway DB.

Vì sao xảy ra?
Bạn có thể đang có:

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>
        spring-boot-starter-quartz
    </artifactId>
</dependency>
Khi Spring thấy Quartz:

start quartzScheduler
        |
        |
 JDBC JobStore
        |
        |
 tìm QRTZ_LOCKS
nhưng H2 test DB sạch → fail.

Setup mình khuyên với project Spring Boot của bạn:
application-test.properties
spring.quartz.auto-startup=false

spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=create-drop
application-prod.properties
spring.quartz.job-store-type=jdbc

spring.quartz.jdbc.initialize-schema=never
Railway PostgreSQL giữ:

QRTZ_*
để sau này deploy nhiều EC2:

EC2-1
 |
 Quartz
 |
 Railway DB
 |
EC2-2
 |
 Quartz
chỉ một instance chạy job.

Hiện tại CI/CD của bạn fail ở bước Maven test, nên chỉ cần thêm:

spring.quartz.auto-startup=false
vào profile test là build sẽ qua.


Error: Tests run: 1, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: 17.29 s <<< FAILURE! -- in vn.campuslife.CampusLifeApplicationTests
Error: vn.campuslife.CampusLifeApplicationTests.contextLoads -- Time elapsed: 0.008 s <<< ERROR!
java.lang.IllegalStateException: Failed to load ApplicationContext for [WebMergedContextConfiguration@4e0c40f3 testClass = vn.campuslife.CampusLifeApplicationTests, locations = [], classes = [vn.campuslife.CampusLifeApplication], contextInitializerClasses = [], activeProfiles = ["test"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@21d03963, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@33f676f6, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@7b50df34, org.springframework.boot.test.web.reactor.netty.DisableReactorResourceFactoryGlobalResourcesContextCustomizerFactory$DisableReactorResourceFactoryGlobalResourcesContextCustomizerCustomizer@75c56eb9, org.springframework.boot.test.autoconfigure.OnFailureConditionReportContextCustomizerFactory$OnFailureConditionReportContextCustomizer@6531a794, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@0, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@7d94beb9, org.springframework.test.context.support.DynamicPropertiesContextCustomizer@0, org.springframework.boot.test.context.SpringBootTestAnnotation@265b64f7], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:180)
at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130)
at org.springframework.test.context.web.ServletTestExecutionListener.setUpRequestContextIfNecessary(ServletTestExecutionListener.java:200)
at org.springframework.test.context.web.ServletTestExecutionListener.prepareTestInstance(ServletTestExecutionListener.java:139)
at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260)
at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:159)
at java.base/java.util.stream.ForEachOps$ForEachOp$OfRef.accept(ForEachOps.java:184)
at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179)
at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708)
at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509)
at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499)
at java.base/java.util.stream.ForEachOps$ForEachOp.evaluateSequential(ForEachOps.java:151)
at java.base/java.util.stream.ForEachOps$ForEachOp$OfRef.evaluateSequential(ForEachOps.java:174)
at java.base/java.util.stream.AbstractPipeline.evaluate(AbstractPipeline.java:234)
at java.base/java.util.stream.ReferencePipeline.forEach(ReferencePipeline.java:596)
at java.base/java.util.Optional.orElseGet(Optional.java:364)
at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
Caused by: org.springframework.context.ApplicationContextException: Failed to start bean 'quartzScheduler'
at org.springframework.context.support.DefaultLifecycleProcessor.doStart(DefaultLifecycleProcessor.java:408)
at org.springframework.context.support.DefaultLifecycleProcessor.doStart(DefaultLifecycleProcessor.java:394)
at org.springframework.context.support.DefaultLifecycleProcessor$LifecycleGroup.start(DefaultLifecycleProcessor.java:586)
at java.base/java.lang.Iterable.forEach(Iterable.java:75)
at org.springframework.context.support.DefaultLifecycleProcessor.startBeans(DefaultLifecycleProcessor.java:364)
at org.springframework.context.support.DefaultLifecycleProcessor.onRefresh(DefaultLifecycleProcessor.java:310)
at org.springframework.context.support.AbstractApplicationContext.finishRefresh(AbstractApplicationContext.java:1006)
at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:630)
at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:752)
at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:439)
at org.springframework.boot.SpringApplication.run(SpringApplication.java:318)
at org.springframework.boot.test.context.SpringBootContextLoader.lambda$loadContext$3(SpringBootContextLoader.java:144)
at org.springframework.util.function.ThrowingSupplier.get(ThrowingSupplier.java:58)
at org.springframework.util.function.ThrowingSupplier.get(ThrowingSupplier.java:46)
at org.springframework.boot.SpringApplication.withHook(SpringApplication.java:1461)
at org.springframework.boot.test.context.SpringBootContextLoader$ContextLoaderHook.run(SpringBootContextLoader.java:563)
at org.springframework.boot.test.context.SpringBootContextLoader.loadContext(SpringBootContextLoader.java:144)
at org.springframework.boot.test.context.SpringBootContextLoader.loadContext(SpringBootContextLoader.java:110)
at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContextInternal(DefaultCacheAwareContextLoaderDelegate.java:225)
at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:152)
... 19 more
Caused by: org.springframework.scheduling.SchedulingException: Could not start Quartz Scheduler
at org.springframework.scheduling.quartz.SchedulerFactoryBean.start(SchedulerFactoryBean.java:803)
at org.springframework.context.support.DefaultLifecycleProcessor.doStart(DefaultLifecycleProcessor.java:405)
... 38 more
Caused by: org.quartz.SchedulerConfigException: Failure occurred during job recovery. [See nested exception: org.quartz.impl.jdbcjobstore.LockException: Failure obtaining db row lock: Table "QRTZ_LOCKS" not found; SQL statement:
SELECT * FROM QRTZ_LOCKS WHERE SCHED_NAME = 'quartzScheduler' AND LOCK_NAME = ? FOR UPDATE [42102-232] [See nested exception: org.h2.jdbc.JdbcSQLSyntaxErrorException: Table "QRTZ_LOCKS" not found; SQL statement:
SELECT * FROM QRTZ_LOCKS WHERE SCHED_NAME = 'quartzScheduler' AND LOCK_NAME = ? FOR UPDATE [42102-232]]]
at org.quartz.impl.jdbcjobstore.JobStoreSupport.schedulerStarted(JobStoreSupport.java:698)
at org.quartz.core.QuartzScheduler.start(QuartzScheduler.java:533)
at org.quartz.impl.StdScheduler.start(StdScheduler.java:143)
at org.springframework.scheduling.quartz.SchedulerFactoryBean.startScheduler(SchedulerFactoryBean.java:731)
at org.springframework.scheduling.quartz.SchedulerFactoryBean.start(SchedulerFactoryBean.java:800)
... 39 more
Caused by: org.quartz.impl.jdbcjobstore.LockException: Failure obtaining db row lock: Table "QRTZ_LOCKS" not found; SQL statement:
SELECT * FROM QRTZ_LOCKS WHERE SCHED_NAME = 'quartzScheduler' AND LOCK_NAME = ? FOR UPDATE [42102-232] [See nested exception: org.h2.jdbc.JdbcSQLSyntaxErrorException: Table "QRTZ_LOCKS" not found; SQL statement:
SELECT * FROM QRTZ_LOCKS WHERE SCHED_NAME = 'quartzScheduler' AND LOCK_NAME = ? FOR UPDATE [42102-232]]
at org.quartz.impl.jdbcjobstore.StdRowLockSemaphore.executeSQL(StdRowLockSemaphore.java:185)
at org.quartz.impl.jdbcjobstore.DBSemaphore.obtainLock(DBSemaphore.java:112)
at org.quartz.impl.jdbcjobstore.JobStoreSupport.executeInNonManagedTXLock(JobStoreSupport.java:3729)
at org.quartz.impl.jdbcjobstore.JobStoreSupport.recoverJobs(JobStoreSupport.java:836)
at org.quartz.impl.jdbcjobstore.JobStoreSupport.schedulerStarted(JobStoreSupport.java:696)
... 43 more
Caused by: org.h2.jdbc.JdbcSQLSyntaxErrorException: Table "QRTZ_LOCKS" not found; SQL statement:
SELECT * FROM QRTZ_LOCKS WHERE SCHED_NAME = 'quartzScheduler' AND LOCK_NAME = ? FOR UPDATE [42102-232]
at org.h2.message.DbException.getJdbcSQLException(DbException.java:514)
at org.h2.message.DbException.getJdbcSQLException(DbException.java:489)
at org.h2.message.DbException.get(DbException.java:223)
at org.h2.message.DbException.get(DbException.java:199)
at org.h2.command.Parser.getTableOrViewNotFoundDbException(Parser.java:7945)
at org.h2.command.Parser.getTableOrViewNotFoundDbException(Parser.java:7916)
at org.h2.command.Parser.readTableOrView(Parser.java:7895)
at org.h2.command.Parser.readTablePrimary(Parser.java:1769)
at org.h2.command.Parser.readTableReference(Parser.java:2249)
at org.h2.command.Parser.parseSelectFromPart(Parser.java:2702)
at org.h2.command.Parser.parseSelect(Parser.java:2810)
at org.h2.command.Parser.parseQueryPrimary(Parser.java:2692)
at org.h2.command.Parser.parseQueryTerm(Parser.java:2547)
at org.h2.command.Parser.parseQueryExpressionBody(Parser.java:2526)
at org.h2.command.Parser.parseQueryExpressionBodyAndEndOfQuery(Parser.java:2519)
at org.h2.command.Parser.parseQueryExpression(Parser.java:2512)
at org.h2.command.Parser.parseQuery(Parser.java:2479)
at org.h2.command.Parser.parsePrepared(Parser.java:610)
at org.h2.command.Parser.parse(Parser.java:581)
at org.h2.command.Parser.parse(Parser.java:556)
at org.h2.command.Parser.prepareCommand(Parser.java:484)
at org.h2.engine.SessionLocal.prepareLocal(SessionLocal.java:645)
at org.h2.engine.SessionLocal.prepareCommand(SessionLocal.java:561)
at org.h2.jdbc.JdbcConnection.prepareCommand(JdbcConnection.java:1164)
at org.h2.jdbc.JdbcPreparedStatement.(JdbcPreparedStatement.java:93)
at org.h2.jdbc.JdbcConnection.prepareStatement(JdbcConnection.java:315)
at com.zaxxer.hikari.pool.ProxyConnection.prepareStatement(ProxyConnection.java:328)
at com.zaxxer.hikari.pool.HikariProxyConnection.prepareStatement(HikariProxyConnection.java)
at java.base/java.lang.reflect.Method.invoke(Method.java:580)
at org.quartz.impl.jdbcjobstore.AttributeRestoringConnectionInvocationHandler.invoke(AttributeRestoringConnectionInvocationHandler.java:78)
at jdk.proxy2/jdk.proxy2.$Proxy248.prepareStatement(Unknown Source)
at org.quartz.impl.jdbcjobstore.StdRowLockSemaphore.executeSQL(StdRowLockSemaphore.java:116)
... 47 more

[INFO]
[INFO] Results:
[INFO]
Error: Errors:
Error: CampusLifeApplicationTests.contextLoads » IllegalState Failed to load ApplicationContext for [WebMergedContextConfiguration@4e0c40f3 testClass = vn.campuslife.CampusLifeApplicationTests, locations = [], classes = [vn.campuslife.CampusLifeApplication], contextInitializerClasses = [], activeProfiles = ["test"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true"], contextCustomizers = [org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@21d03963, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@33f676f6, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@7b50df34, org.springframework.boot.test.web.reactor.netty.DisableReactorResourceFactoryGlobalResourcesContextCustomizerFactory$DisableReactorResourceFactoryGlobalResourcesContextCustomizerCustomizer@75c56eb9, org.springframework.boot.test.autoconfigure.OnFailureConditionReportContextCustomizerFactory$OnFailureConditionReportContextCustomizer@6531a794, org.springframework.boot.test.autoconfigure.actuate.observability.ObservabilityContextCustomizerFactory$DisableObservabilityContextCustomizer@1f, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@0, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizer@7d94beb9, org.springframework.test.context.support.DynamicPropertiesContextCustomizer@0, org.springframework.boot.test.context.SpringBootTestAnnotation@265b64f7], resourceBasePath = "src/main/webapp", contextLoader = org.springframework.boot.test.context.SpringBootContextLoader, parent = null]
[INFO]
Error: Tests run: 1, Failures: 0, Errors: 1, Skipped: 0
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 01:40 min
[INFO] Finished at: 2026-06-20T17:00:09Z
[INFO] ------------------------------------------------------------------------
Error: Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:3.5.3 (default-test) on project campuslife:
Error:
Error: See /home/runner/work/CampusLife/CampusLife/target/surefire-reports for the individual test results.
Error: See dump files (if any exist) [date].dump, [date]-jvmRun[N].dump and [date].dumpstream.
Error: -> [Help 1]
Error:
Error: To see the full stack trace of the errors, re-run Maven with the -e switch.
Error: Re-run Maven using the -X switch to enable full debug logging.
Error:
Error: For more information about the errors and possible solutions, please read the following articles:
Error: [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException
Error: Process completed with exit code 1.


Close
