package org.iamzhongyong.profiler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 添加标记之后，进行统计时间
 * 可以是方法，可以是类
 * 
 * 使用步骤：
 * 1、在需要做Profiler的地方，添加@ProfilerAnno 这个注解，可以在方法上或者类上面；
 * 2、系统默认是关闭的，也就是默认不开放这个功能；
 * 3、如果需要开始做，可以通过curl开关搞一下，开关位置在：ProfilerSwitch；
 * 4、如果开关打开，默认是500ms的时候，才会打印日志，如果想调整这个时间，可以动态调整：ProfilerSwitch；
 * 5、日志文件可以进行指定；
 * 6、如果是看性能或者排查问题，搞完了之后，开关记得关闭；
 */
@Target({ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ProfilerAnno {
	String desc() default "";
}
