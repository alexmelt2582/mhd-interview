package com.mhd.interview.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 应用启动入口
 */
@SpringBootApplication(scanBasePackages = {"com.mhd.interview.web","com.mhd.interview.ai"})
@Slf4j
public class InterviewApplication {

	/**
	 * 启动应用
	 *
	 * @param args 启动参数
	 */
	public static void main(String[] args) {
		SpringApplication.run(InterviewApplication.class, args);
		log.info("interview-business started successfully.");
	}
}
