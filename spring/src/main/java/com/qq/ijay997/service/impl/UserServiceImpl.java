package com.qq.ijay997.service.impl;

import com.qq.ijay997.entity.User;
import com.qq.ijay997.repository.UserRepository;
import com.qq.ijay997.service.OrderService;
import com.qq.ijay997.service.UserService;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

/**
 * 用户服务实现类
 *
 * @author ijay997
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Resource
    private ObjectFactory<OrderService> objectFactory;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Resource
    private PlatformTransactionManager transactionManager;

    @Override
    public List<User> findAll() {
        objectFactory.getObject().getOrder();
        return userRepository.findAll();
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public User save(User user) {
        return transactionTemplate.execute(transactionStatus -> {
            User newUser = userRepository.save(user);
//            transactionStatus.setRollbackOnly();
            return newUser;
        });
    }

    // 方式 2: 使用 PlatformTransactionManager
    public void processWithManager() {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        // 手动获取 TransactionStatus（需要自己管理提交/回滚）
        TransactionStatus status = transactionManager.getTransaction(def);
        try {
            // 业务逻辑
            transactionManager.commit(status);
        } catch (Exception e) {
            transactionManager.rollback(status);
        }
    }

    // 方式 4: 自定义 TransactionTemplate 的事务属性
    public User saveWithCustomTransaction(User user) {
        // 创建新的 TransactionTemplate（或注入后修改属性）
        TransactionTemplate customTemplate = new TransactionTemplate(transactionManager);
        
        // 设置事务属性
        customTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED); // 隔离级别
        customTemplate.setTimeout(30);  // 超时时间（秒）
        customTemplate.setReadOnly(false);  // 是否只读
        customTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED); // 传播行为
        
        return customTemplate.execute(transactionStatus -> {
            System.out.println("=== 自定义事务属性 ===");
            System.out.println("当前有活跃事务：" + TransactionSynchronizationManager.isActualTransactionActive());
            System.out.println("只读：" + TransactionSynchronizationManager.isCurrentTransactionReadOnly());
            
            User newUser = userRepository.save(user);
            return newUser;
        });
    }

    // 方式 5: 不同的事务属性配置示例
    public void differentTransactionScenarios() {
        
        // 场景 1: 只读事务（优化查询性能）
        TransactionTemplate readOnlyTemplate = new TransactionTemplate(transactionManager);
        readOnlyTemplate.setReadOnly(true);
        readOnlyTemplate.execute(status -> {
            // 查询操作，不会修改数据
            List<User> users = userRepository.findAll();
            System.out.println("只读事务中查询到 " + users.size() + " 个用户");
            return null;
        });
        
        // 场景 2: 需要新事务（独立于当前事务）
        TransactionTemplate requiresNewTemplate = new TransactionTemplate(transactionManager);
        requiresNewTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        requiresNewTemplate.execute(status -> {
            // 总是创建新事务，即使外层有事务也会挂起外层
            System.out.println("独立事务执行");
            return null;
        });
        
        // 场景 3: 嵌套事务（可以单独回滚）
        TransactionTemplate nestedTemplate = new TransactionTemplate(transactionManager);
        nestedTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
        nestedTemplate.execute(status -> {
            System.out.println("嵌套事务执行");
            // 可以在这里设置保存点，部分回滚
            return null;
        });
        
        // 场景 4: 非事务执行（挂起当前事务）
        TransactionTemplate notSupportedTemplate = new TransactionTemplate(transactionManager);
        notSupportedTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
        notSupportedTemplate.execute(status -> {
            System.out.println("非事务方式执行");
            return null;
        });
    }

    // 方式 6: 动态设置事务属性（根据业务条件）
    public User saveUserWithDynamicTransaction(User user, boolean isHighPriority) {
        TransactionTemplate dynamicTemplate = new TransactionTemplate(transactionManager);
        
        // 根据业务条件动态调整事务属性
        if (isHighPriority) {
            // 高优先级：更高的隔离级别，更长的超时时间
            dynamicTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
            dynamicTemplate.setTimeout(60);
            System.out.println("高优先级事务：串行化隔离级别，60 秒超时");
        } else {
            // 普通优先级：默认隔离级别，标准超时
            dynamicTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
            dynamicTemplate.setTimeout(30);
            System.out.println("普通事务：读已提交隔离级别，30 秒超时");
        }
        
        return dynamicTemplate.execute(status -> {
            return userRepository.save(user);
        });
    }

    // 方式 3: 使用 TransactionSynchronizationManager（只读信息）
    public void checkTransactionInfo() {
        // 静态方法获取事务信息（不能控制事务）
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            System.out.println("当前在事务中");
        }
    }

    @Override
    public User update(Long id, User userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在，ID: " + id));

        user.setUsername(userDetails.getUsername());
        user.setEmail(userDetails.getEmail());
        user.setAge(userDetails.getAge());

        return userRepository.save(user);
    }

    @Override
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public List<User> findByAgeRange(Integer minAge, Integer maxAge) {
        return userRepository.findByAgeBetween(minAge, maxAge);
    }
}
