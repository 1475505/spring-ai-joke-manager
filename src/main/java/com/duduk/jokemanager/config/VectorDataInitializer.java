package com.duduk.jokemanager.config;

import com.duduk.jokemanager.entity.Joke;
import com.duduk.jokemanager.repository.JokeRepository;
import com.duduk.jokemanager.repository.VectorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 向量数据库初始化器
 * 在应用启动时将现有的笑话数据同步到向量数据库
 */
@Component
public class VectorDataInitializer implements ApplicationRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(VectorDataInitializer.class);
    
    @Autowired
    private JokeRepository jokeRepository;
    
    @Autowired
    private VectorRepository vectorRepository;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("开始初始化向量数据库...");
        
        try {
            // 获取所有已批准的笑话（包含主题关联）
        List<Joke> approvedJokes = jokeRepository.findAllApprovedWithTheme();
            
            if (!approvedJokes.isEmpty()) {
                logger.info("找到 {} 条已批准的笑话，开始同步到向量数据库", approvedJokes.size());
                
                // 批量添加到向量数据库
                vectorRepository.addJokes(approvedJokes);
                
                logger.info("向量数据库初始化完成，共同步 {} 条笑话", approvedJokes.size());
            } else {
                logger.info("没有找到已批准的笑话，跳过向量数据库初始化");
            }
            
        } catch (Exception e) {
            logger.error("向量数据库初始化失败: {}", e.getMessage(), e);
            // 不抛出异常，避免影响应用启动
        }
    }
}