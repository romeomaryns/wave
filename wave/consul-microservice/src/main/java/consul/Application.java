/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package consul;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.bus.jackson.SubtypeModule;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.consul.bus.SimpleRemoteEvent;
import org.springframework.cloud.ui.EnableConsulUi;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;


@SpringBootApplication
@EnableDiscoveryClient
@EnableConsulUi
@RestController
@EnableConfigurationProperties
@Slf4j
public class Application implements ApplicationListener<SimpleRemoteEvent> {

	final Logger logger = LoggerFactory.getLogger(Application.class);

	@Autowired
	private LoadBalancerClient loadBalancer;

	@Autowired
	private DiscoveryClient discoveryClient;

	@Autowired
	private Environment env;

	@Autowired(required = false)
	private RelaxedPropertyResolver resolver;

	@Value("${spring.application.name:consul}")
	private String appName;

	@PostConstruct
	public void init() {
		if (resolver == null) {
			resolver = new RelaxedPropertyResolver(env);
		}
	}

	@RequestMapping("/me")
	public ServiceInstance me() {
		return discoveryClient.getLocalServiceInstance();
	}

	@RequestMapping("/")
	public ServiceInstance lb() {
		return loadBalancer.choose(appName);
	}

	@RequestMapping("/myenv")
	public String env(@RequestParam("prop") String prop) {
		return resolver.getProperty(prop, "Not Found");
	}

	@RequestMapping("/prop")
	public String prop() {
		return sampleProperties().getProp();
	}

	@Bean
	public SubtypeModule sampleSubtypeModule() {
		return new SubtypeModule(SimpleRemoteEvent.class);
	}

	@Bean
	public ConsulProperties sampleProperties() {
		return new ConsulProperties();
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Override
	public void onApplicationEvent(SimpleRemoteEvent event) {
		logger.info("Received event: {}", event);
	}
}
