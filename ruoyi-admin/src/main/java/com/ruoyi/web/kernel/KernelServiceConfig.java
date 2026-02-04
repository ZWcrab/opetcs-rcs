package com.ruoyi.web.kernel;

import org.opentcs.access.KernelServicePortal;
import org.opentcs.access.rmi.KernelServicePortalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class KernelServiceConfig {

  private static final Logger log = LoggerFactory.getLogger(KernelServiceConfig.class);

  /**
   * 缓存一个 KernelServicePortal 实例，避免重复创建。
   * 注意：这里仅负责创建与登录，不在项目启动阶段强制要求连接成功。
   */
  private KernelServicePortal kernelServicePortal;

  /**
   * 懒加载的 KernelServicePortal Bean。
   * <p>
   * - 使用 {@link Lazy} 避免在 Spring 启动阶段就发起连接；
   * - 使用 try/catch 包裹 login，连接失败时仅记录日志，不影响项目启动；
   * - openTCS 不可用时，后续真正调用相关 Service 时会抛出业务异常，由上层感知。
   */
  @Bean
  @Lazy
  public KernelServicePortal kernelServicePortal() {
    if (kernelServicePortal == null) {
      // 在此处配置连接参数，例如用户名、密码等
      kernelServicePortal = new KernelServicePortalBuilder("Alice", "xyz").build();
      try {
        kernelServicePortal.login("127.0.0.1", 1099);
      } catch (Exception e) {
        log.warn("openTCS 内核连接失败，相关功能将不可用，请检查 openTCS 服务是否已启动。", e);
      }
    }
    return kernelServicePortal;
  }
}
