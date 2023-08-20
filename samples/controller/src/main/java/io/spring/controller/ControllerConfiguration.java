package io.spring.controller;

import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.spring.controller.models.V1Foo;
import io.spring.controller.models.V1FooList;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import io.kubernetes.client.openapi.ApiClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.CommandLineRunner;
import java.util.concurrent.Executors;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import java.time.Duration;
import io.kubernetes.client.openapi.models.*;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;

@RegisterReflectionForBinding({ V1FooList.class, V1ConfigMapList.class, V1DeploymentList.class })
@Configuration
public class ControllerConfiguration {

    @Bean
    GenericKubernetesApi<V1Foo, V1FooList> foosApi(ApiClient apiClient) {
      return new GenericKubernetesApi<>(V1Foo.class, V1FooList.class, "spring.io", "v1", "foos", apiClient);
    }

    @Bean
    SharedIndexInformer<V1Foo> foosSharedIndexInformer(SharedInformerFactory sharedInformerFactory,
        GenericKubernetesApi<V1Foo, V1FooList> api) {
      return sharedInformerFactory.sharedIndexInformerFor(api, V1Foo.class, 0);
    }
    
    @Bean
    AppsV1Api appsV1Api(ApiClient apiClient) {
        return new AppsV1Api(apiClient);
    }

    @Bean
    CoreV1Api coreV1Api(ApiClient apiClient) {
        return new CoreV1Api(apiClient);
    }

    @Bean
    Reconciler reconciler(SharedIndexInformer<V1Foo> parentInformer,
                          CoreV1Api coreV1Api,
                          AppsV1Api appsV1Api) {
        return new FooReconciler(parentInformer, coreV1Api, appsV1Api);
    }
    
    @Bean
    Controller controller(SharedInformerFactory sharedInformerFactory,
                          SharedIndexInformer<V1Foo> informer,
                          Reconciler reconciler) {
        return ControllerBuilder
                .defaultBuilder(sharedInformerFactory)
                .watch(q -> ControllerBuilder
                        .controllerWatchBuilder(V1Foo.class, q)
                        .withResyncPeriod(Duration.ofSeconds(30))
                        .build())
                .withReconciler(reconciler)
                .withReadyFunc(informer::hasSynced)
                .withWorkerCount(2)
                .build();
    }

    @Bean
    public CommandLineRunner commandLineRunner(SharedInformerFactory sharedInformerFactory, Controller controller) {
        return args -> Executors.newSingleThreadExecutor().execute(() -> {
            sharedInformerFactory.startAllRegisteredInformers();
            controller.run();
        });
    }
}
