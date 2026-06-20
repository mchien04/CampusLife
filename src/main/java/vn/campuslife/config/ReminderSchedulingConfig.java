package vn.campuslife.config;

import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

@Configuration
public class ReminderSchedulingConfig {

    @Bean
    public SchedulerFactoryBeanCustomizer schedulerFactoryBeanCustomizer(ApplicationContext applicationContext) {
        return schedulerFactoryBean -> {
            schedulerFactoryBean.setJobFactory(
                    new AutowiringSpringBeanJobFactory(applicationContext.getAutowireCapableBeanFactory()));
            schedulerFactoryBean.setWaitForJobsToCompleteOnShutdown(true);
        };
    }

    private static final class AutowiringSpringBeanJobFactory extends SpringBeanJobFactory {

        private final AutowireCapableBeanFactory beanFactory;

        private AutowiringSpringBeanJobFactory(AutowireCapableBeanFactory beanFactory) {
            this.beanFactory = beanFactory;
        }

        @Override
        protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
            Object job = super.createJobInstance(bundle);
            beanFactory.autowireBean(job);
            return job;
        }
    }
}
