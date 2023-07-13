The last thing we need in our code is all of the imports:

```editor:insert-lines-before-line
file: samples/src/main/java/io/spring/ControllersApplication.java
line: 6
text: |
    import io.spring.models.V1Foo;
    import io.spring.models.V1FooList;
    import io.kubernetes.client.common.KubernetesObject;
    import io.kubernetes.client.extended.controller.Controller;
    import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
    import io.kubernetes.client.extended.controller.builder.DefaultControllerBuilder;
    import io.kubernetes.client.extended.controller.reconciler.Reconciler;
    import io.kubernetes.client.extended.controller.reconciler.Result;
    import io.kubernetes.client.informer.SharedIndexInformer;
    import io.kubernetes.client.informer.SharedInformerFactory;
    import io.kubernetes.client.openapi.ApiClient;
    import io.kubernetes.client.openapi.ApiException;
    import io.kubernetes.client.openapi.apis.AppsV1Api;
    import io.kubernetes.client.openapi.apis.CoreV1Api;
    import io.kubernetes.client.openapi.models.*;
    import io.kubernetes.client.util.Yaml;
    import io.kubernetes.client.util.generic.GenericKubernetesApi;
    import lombok.SneakyThrows;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.aot.hint.RuntimeHints;
    import org.springframework.aot.hint.RuntimeHintsRegistrar;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.boot.ApplicationRunner;
    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.ImportRuntimeHints;
    import org.springframework.core.io.ClassPathResource;
    import org.springframework.core.io.Resource;
    import org.springframework.util.Assert;
    import org.springframework.util.FileCopyUtils;

    import java.io.InputStreamReader;
    import java.time.Duration;
    import java.time.Instant;
    import java.util.List;
    import java.util.Map;
    import java.util.Objects;
    import java.util.concurrent.Executors;

    @Slf4j
    @ImportRuntimeHints(ControllersApplication.FooControllerRuntimeHints.class)
```