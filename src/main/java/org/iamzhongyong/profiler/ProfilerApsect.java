package org.iamzhongyong.profiler;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 用于埋点的拦截器
 */
@Aspect
public class ProfilerApsect {
	
	private final static Logger logger = LoggerFactory.getLogger(ProfilerApsect.class);
	

	@Around("@annotation(org.iamzhongyong.profiler.ProfilerAnno) "
			+ "|| @within(org.iamzhongyong.profiler.ProfilerAnno)")
	public Object invoke(final ProceedingJoinPoint joinPoint) throws Throwable {
		if(!ProfilerSwitch.getInstance().isOpenProfilerTree()){
			return joinPoint.proceed();
		}
		String methodName = this.getClassAndMethodName(joinPoint);
		if(null==methodName){
			return joinPoint.proceed();
		}
		try{
			if(Profiler.getEntry()==null){
				Profiler.start(methodName);
			}else{
				Profiler.enter(methodName);
			}
			return joinPoint.proceed();
		}catch(Throwable e){
			throw e;
		}finally{
			Profiler.release();
			//当root entry为状态为release的时候，打印信息，并做reset操作
			Profiler.Entry rootEntry = Profiler.getEntry();
			if(rootEntry!=null){
				if(rootEntry.isReleased()){
					long duration = rootEntry.getDuration();
					if(duration > ProfilerSwitch.getInstance().getInvokeTimeout()){
						logger.error("\n"+Profiler.dump()+"\n");		
						
					}
					Profiler.reset();
				}
			}
		}		
	}
	
	private String getClassAndMethodName(ProceedingJoinPoint joinPoint){
		try{
			MethodSignature sign = (MethodSignature)joinPoint.getSignature();
			String clazzName = joinPoint.getTarget().toString();
			StringBuilder sb = new StringBuilder();
			sb.append(Profiler.split(clazzName, "@")[0]);
			sb.append(":").append(sign.getMethod().getName());
			sb.append("(param:").append(sign.getMethod().getParameterTypes().length);
			sb.append(")");
			return sb.toString();
		}catch(Throwable e){
			return null;
		}
	}
}
