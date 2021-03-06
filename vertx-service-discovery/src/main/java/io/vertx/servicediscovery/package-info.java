/*
 * Copyright (c) 2011-2016 The original author or authors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 *      The Eclipse Public License is available at
 *      http://www.eclipse.org/legal/epl-v10.html
 *
 *      The Apache License v2.0 is available at
 *      http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

/**
 * = Vert.x Service Discovery
 * 
 * This components provides an infrastructure to publish and discover various resources, such as service
 * proxies, HTTP endpoints, data sources... These resources are called `services`. A `service` is a discoverable
 * functionality. It can be qualified by its type, metadata, and location. So a `service` can be a database, a
 * service proxy, a HTTP endpoint and any other resource you can imagine as soon as you can describe it and interact
 * with it. It does not have to be a vert.x entity, but can be anything. Each service is described by a
 * {@link io.vertx.servicediscovery.Record}.
 * 
 * The service discovery implements the interactions defined in the service-oriented computing. And to some extend,
 * also provides the dynamic service-oriented computing interactions. So, applications can react to arrival and
 * departure of services.
 * 
 * A service provider can:
 * 
 * * publish a service record
 * * un-publish a published record
 * * update the status of a published service (down, out of service...)
 * 
 * A service consumer can:
 * 
 * * lookup for services
 * * bind to a selected service (it gets a {@link io.vertx.servicediscovery.ServiceReference}) and use it
 * * release the service once the consumer is done with it
 * * listen for arrival, departure and modification of services.
 * 
 * Consumer would 1) lookup for service record matching their need, 2) retrieve the
 * {@link io.vertx.servicediscovery.ServiceReference} that give access to the service, 3) get a service object to access
 * the service, 4) release the service object once done.
 * 
 * A state above, the central piece of information shared by the providers and consumers are
 * {@link io.vertx.servicediscovery.Record records}.
 * 
 * Providers and consumers must create their own {@link io.vertx.servicediscovery.ServiceDiscovery} instance. These
 * instances are collaborating in background (distributed structure) to keep the set of services in sync.
 * 
 * The service discovery supports bridges to import and export services from / to other discovery technologies.
 * 
 * == Using the service discovery
 * 
 * To use the Vert.x service discovery, add the following dependency to the _dependencies_ section of your build
 * descriptor:
 * 
 * * Maven (in your `pom.xml`):
 * 
 * [source,xml,subs="+attributes"]
 * ----
 * <dependency>
 * <groupId>${maven.groupId}</groupId>
 * <artifactId>${maven.artifactId}</artifactId>
 * <version>${maven.version}</version>
 * </dependency>
 * ----
 * 
 * * Gradle (in your `build.gradle` file):
 * 
 * [source,groovy,subs="+attributes"]
 * ----
 * compile '${maven.groupId}:${maven.artifactId}:${maven.version}'
 * ----
 * 
 * == Overall concepts
 * 
 * The discovery mechanism is based on a few concepts explained in this section.
 * 
 * === Service records
 * 
 * A service {@link io.vertx.servicediscovery.Record} is an object that describes a service published by a service
 * provider. It contains a name, some metadata, a location object (describing where is the service). This record is
 * the only objects shared by the provider (having published it) and the consumer (retrieve it when doing a lookup).
 * 
 * The metadata and even the location format depends on the `service type` (see below).
 * 
 * A record is published when the provider is ready to be used, and withdrawn when the service provider is stopping.
 * 
 * === Service Provider and publisher
 * 
 * A service provider is an entity providing a _service_. The publisher is responsible for publishing a record
 * describing the provider. It may be a single entity (a provider publishing itself) or a different entity.
 * 
 * === Service Consumer
 * 
 * Service consumers search for services in the service discovery. Each lookup retrieves `0..n`
 * {@link io.vertx.servicediscovery.Record}. From these records, a consumer can retrieve a
 * {@link io.vertx.servicediscovery.ServiceReference}, representing the binding between the consumer and the provider.
 * This reference allows the consumer to retrieve the _service object_ (to use the service),  and release the service.
 *
 * It is important to release service references to cleanup the objects and update the service usages.
 * 
 * === Service object
 * 
 * The service object is the object that give access to a service. It has various form, such as a proxy, a client, or
 * may even be non existent for some service type. The nature of the service object depends of the service type.
 * 
 * === Service types
 * 
 * Services are resources, and it exists a wide variety of resources. They can be functional services, databases,
 * REST APIs, and so on. The Vert.x service discovery has the concept of service types to handle this heterogeneity.
 * Each type defines:
 * 
 * * how the service is located (URI, event bus address, IP / DNS...)
 * * the nature of the service object (service proxy, HTTP client, message consumer...)
 * 
 * Some service types are implemented and provided by the service discovery component, but you can add
 * your own.
 * 
 * === Service events
 * 
 * Every time a service provider is published or withdrawn, an event is fired on the event bus. This event contains
 * the record that has been modified.
 *
 * In addition, in order to track who is using who, every time a reference is retrieved with
 * {@link io.vertx.servicediscovery.ServiceDiscovery#getReference(Record)} or released with
 * {@link io.vertx.servicediscovery.ServiceReference#release()}, events are emitted on the event bus to track the
 * service usages.
 *
 * More details on these events below.
 * 
 * === Backend
 * 
 * The service discovery uses a distributed structure to store the records. So, all members of the cluster have access
 * to all the records. This is the default backend implementation. You can implement your own by implementing the
 * {@link io.vertx.servicediscovery.spi.ServiceDiscoveryBackend} SPI.
 *
 * Notice that the discovery does not require vert.x clustering. In single-node mode, the map is a local map. It can
 * be populated with {@link io.vertx.servicediscovery.spi.ServiceImporter}s.
 * 
 * == Creating a service discovery instance
 * 
 * Publishers and consumers must create their own {@link io.vertx.servicediscovery.ServiceDiscovery}
 * instance to use the discovery infrastructure:
 * 
 * [source,$lang]
 * ----
 * {@link examples.Examples#example1(io.vertx.core.Vertx)}
 * ----
 * 
 * By default, the announce address (the event bus address on which service events are sent is: `vertx.discovery
 * .announce`. You can also configure a name used for the service usage (see section about service usage).
 * 
 * When you don't need the service discovery object anymore, don't forget to close it. It closes the different discovery bridges you
 * have configured and releases the service references.
 * 
 * == Publishing services
 * 
 * Once you have a service discovery instance, you can start to publish services. The process is the following:
 * 
 * 1. create a record for a specific service provider
 * 2. publish this record
 * 3. keep the published record that is used to un-publish a service or modify it.
 * 
 * To create records, you can either use the {@link io.vertx.servicediscovery.Record} class, or use convenient methods
 * from the service types.
 * 
 * [source,$lang]
 * ----
 * {@link examples.Examples#example2(ServiceDiscovery)}
 * ----
 * 
 * It is important to keep a reference on the returned records, as this record has been extended by a `registration id`.
 * 
 * == Withdrawing services
 * 
 * To withdraw (un-publish) a record, use:
 * 
 * [source,$lang]
 * ----
 * {@link examples.Examples#example3(ServiceDiscovery, Record)}
 * ----
 * 
 * == Looking for service
 * 
 * On the consumer side, the first thing to do is to lookup for records. You can search for a single record or all
 * the matching ones. In the first case, the first matching record is returned.
 * 
 * Consumer can pass a filter to select the service. There are two ways to describe the filter:
 *
 * 1. A function taking a {@link io.vertx.servicediscovery.Record} as parameter and returning a boolean
 * 2. This filter is a JSON object. Each entry of the given filter are checked against the record. All entry must
 * match exactly the record. The entry can use the special `*` value to denotes a requirement on the key, but not on
 * the value.
 * 
 * Let's take some example of JSON filter:
 * ----
 * { "name" = "a" } => matches records with name set fo "a"
 * { "color" = "*" } => matches records with "color" set
 * { "color" = "red" } => only matches records with "color" set to "red"
 * { "color" = "red", "name" = "a"} => only matches records with name set to "a", and color set to "red"
 * ----
 * 
 * If the JSON filter is not set ({@code null} or empty), it accepts all records. When using functions, to accept all
 * records, you must return true regardless the record.
 * 
 * Here are some examples:
 * 
 * [source,$lang]
 * ----
 * {@link examples.Examples#example4(ServiceDiscovery)}
 * ----
 *
 * You can retrieve a single record or all matching record with
 * {@link io.vertx.servicediscovery.ServiceDiscovery#getRecords(io.vertx.core.json.JsonObject, io.vertx.core.Handler)}.
 * By default, record lookup does includes only records with a `status` set to `UP`. This can be overridden:
 *
 * * when using JSON filter, just set `status` to the value you want (or `*` to accept all status)
 * * when using function, set the `includeOutOfService` parameter to `true` in
 * {@link io.vertx.servicediscovery.ServiceDiscovery#getRecords(java.util.function.Function, boolean, io.vertx.core.Handler)}
 * .
 * 
 * == Retrieving a service reference
 * 
 * Once you have chosen the {@link io.vertx.servicediscovery.Record}, you can retrieve a
 * {@link io.vertx.servicediscovery.ServiceReference} and then the service object:
 * 
 * [source,$lang]
 * ----
 * {@link examples.Examples#example5(ServiceDiscovery, Record)}
 * ----
 * 
 * Don't forget to release the reference once done.
 *
 * The service reference represents a binding with the service provider.
 *
 * When retrieving a service reference you can pass a {@link io.vertx.core.json.JsonObject} used to configure the
 * service object. It can contains various data about the service objects. Some service types do not needs additional
 * configuration, some requires configuration (as data sources):
 *
 * [source,$lang]
 * ----
 * {@link examples.Examples#example51(ServiceDiscovery, Record, io.vertx.core.json.JsonObject)}
 * ----
 * 
 * == Types of services
 * 
 * A said above, the service discovery has the service type concept to manage the heterogeneity of the
 * different services.
 * 
 * Are provided by default:
 * 
 * * {@link io.vertx.servicediscovery.types.HttpEndpoint} - for REST API, the service object is a
 * {@link io.vertx.core.http.HttpClient} configured on the host and port (the location is the url).
 * * {@link io.vertx.servicediscovery.types.EventBusService} - for service proxies, the service object is a proxy. Its
 * type is the proxies interface (the location is the address).
 * * {@link io.vertx.servicediscovery.types.MessageSource} - for message source (publisher), the service object is a
 * {@link io.vertx.core.eventbus.MessageConsumer} (the location is the address).
 * * {@link io.vertx.servicediscovery.types.JDBCDataSource} - for JDBC data sources, the service object is a
 * {@link io.vertx.ext.jdbc.JDBCClient} (the configuration of the client is computed from the location, metadata and
 * consumer configuration).
 *
 * This section gives details about service types and describes how can be used the default service types.
 *
 * === Services with no type
 *
 * Some records may have no type ({@link io.vertx.servicediscovery.spi.ServiceType#UNKNOWN}). It is not possible to
 * retrieve a reference for these records, but you can build the connection details from the `location` and
 * `metadata` of the {@link io.vertx.servicediscovery.Record}.
 *
 * Using these services does not fire service usage events.
 *
 * [language, java]
 * ----
 * === Implementing your own service type
 * 
 * You can create your own service type by implementing the {@link io.vertx.servicediscovery.spi.ServiceType} SPI:
 *
 * 1. (optional) Create a public interface extending {@link io.vertx.servicediscovery.spi.ServiceType}. This interface is
 * only used to provide helper methods to ease the usage of your type such as `createRecord` methods, `getX` where `X`
 * is the type of service object you retrieve and so on. Check {@link io.vertx.servicediscovery.types.HttpEndpoint} or
 * {@link io.vertx.servicediscovery.types.MessageSource} for examples
 * 2. Create a class implementing {@link io.vertx.servicediscovery.spi.ServiceType} or the interface you created in the
 * step 1. The type has a `name`, and a method to create the {@link io.vertx.servicediscovery.ServiceReference} for this
 * type. The name must match the `type` field of the {@link io.vertx.servicediscovery.Record} associated with your type.
 * 3. Create a class extending `io.vertx.ext.discovery.types.AbstractServiceReference`. You can parameterized
 * the class with the type of service object your are going to return. You must implement
 * `AbstractServiceReference#retrieve()` that create the service object. This
 * method is only called once. If your service object needs cleanup, also override
 * `AbstractServiceReference#close()`.
 * 4. Create a `META-INF/services/io.vertx.ext.discovery.spi.ServiceType` file that is packaged in your jar. In this
 * file, just indicate the fully qualified name of the class created at step 2.
 * 5. Creates a jar containing the service type interface (step 1), the implementation (step 2 and 3) and the
 * service descriptor file (step 4). Put this jar in the classpath of your application. Here you go, your service
 * type is available.
 * ----
 *
 * === HTTP endpoints
 *
 * A HTTP endpoint represents a REST API or a service accessible using HTTP requests. The HTTP endpoint service
 * objects are {@link io.vertx.core.http.HttpClient} configured with the host, port and ssl.
 *
 * ==== Publishing a HTTP endpoint
 *
 * To publish a HTTP endpoint, you need a {@link io.vertx.servicediscovery.Record}. You can create the record using
 * {@link io.vertx.servicediscovery.types.HttpEndpoint#createRecord(java.lang.String, java.lang.String, int, java.lang.String, io.vertx.core.json.JsonObject)}.
 *
 * The next snippet illustrates hot to create {@link io.vertx.servicediscovery.Record} from
 * {@link io.vertx.servicediscovery.types.HttpEndpoint}:
 *
 * [source, $lang]
 * ----
 * {@link examples.HTTPEndpointExamples#example1(ServiceDiscovery)}
 * ----
 *
 * When you run your service in a container or on the cloud, it may not knows its public IP and public port, so the
 * publication must be done by another entity having this info. Generally it's a bridge.
 *
 * ==== Consuming a HTTP endpoint
 *
 * Once a HTTP endpoint is published, a consumer can retrieve it. The service object is a
 * {@link io.vertx.core.http.HttpClient} with a port and host configured:
 *
 * [source, $lang]
 * ----
 * {@link examples.HTTPEndpointExamples#example2(ServiceDiscovery)}
 * ----
 *
 * You can also use the
 * {@link io.vertx.servicediscovery.types.HttpEndpoint#getClient(ServiceDiscovery, io.vertx.core.json.JsonObject, io.vertx.core.Handler)}
 * method to combine lookup and service retrieval in one call:
 *
 * [source, $lang]
 * ----
 * {@link examples.HTTPEndpointExamples#example3(ServiceDiscovery)}
 * ----
 *
 * In this second version, the service object is released using
 * {@link io.vertx.servicediscovery.ServiceDiscovery#releaseServiceObject(ServiceDiscovery, java.lang.Object)},
 * as you don't hold the service reference.
 *
 * === Event bus services
 *
 * Event bus services are service proxies. They implement async-RPC services on top of the event bus. When retrieved
 * a service object from an event bus service, you get a service proxy in the right type. You can access helper
 * methods from {@link io.vertx.servicediscovery.types.EventBusService}.
 *
 * Notice that service proxies (service implementations and service interfaces) are developed in Java.
 *
 * ==== Publishing an event bus service
 *
 * To publish an event bus service, you need to create a {@link io.vertx.servicediscovery.Record}:
 *
 * [source, $lang]
 * ----
 * {@link examples.EventBusServiceExamples#example1(ServiceDiscovery)}
 * ----
 *
 * [language, java]
 * ----
 * You can also pass the service interface as class too:
 *
 * [source, java]
 * \----
 * {@link examples.limited.EventBusServiceJavaExamples#example1(ServiceDiscovery)}
 * \----
 * ----
 *
 * ==== Consuming an event bus service
 * [language, java]
 * ----
 * To consume an event bus service you can either retrieve the record and then get the reference, or use the
 * {@link io.vertx.servicediscovery.types.EventBusService} interface that combine the two operations in one call.
 *
 * When using the reference, you would do somehting like:
 * [source, java]
 * \----
 * {@link examples.EventBusServiceExamples#example2(ServiceDiscovery)}
 * \----
 *
 * With the {@link io.vertx.servicediscovery.types.EventBusService} class, you can get the proxy as follows:
 * [source, java]
 * \----
 * {@link examples.EventBusServiceExamples#example3(ServiceDiscovery)}
 * \----
 * ----
 * [language, groovy]
 * ----
 * To consume an event bus service you can either retrieve the record and then get the reference, or use the
 * {@link io.vertx.servicediscovery.types.EventBusService} interface that combines the two operations in one call.
 *
 * However, as the service is search by (Java) interface, you need to specify the type of client you expect.
 *
 * [source, groovy]
 * \----
 * def discovery = ServiceDiscovery.create(vertx);
 * EventBusService.<MyService> getProxy(
 *   discovery,
 *   examples.MyService.class.getName(), // service interface
 *   examples.groovy.MyService.class.getName(), // client class
 *   { ar ->
 *      def svc = ar.result();
 *      // ...
 *     ServiceDiscovery.releaseServiceObject(discovery, svc);
 *   }
 * );
 * \----
 * ----
 * [language, js]
 * ----
 * To consume an event bus service, you need to retrieve the record and get the reference as usual. However, as the
 * lookup is made using the java interface (and not the javascript structure) you would need to wrap the service
 * object into the javascript object:
 *
 * [source, javascript]
 * \----
 * var MyService = require("examples-js/my_service.js");
 * var discovery = ServiceDiscovery.create(vertx);
 *
 * discovery.getRecord({"service.interface" : "examples.MyService"},
 *  function(ar, ar_err) {
 *    var reference = discovery.getReference(ar);
 *    var svc = reference.get();
 *    var proxy = new MyService(svc);
 *    proxy.hello({"name" : "vert.x"}, function(r, err) {
 *        // ...
 *        reference.release(); // release the service
 *    });
 * });
 * \----
 * ----
 * [language, ruby]
 * ----
 * TODO
 * ----
 * === Message source
 *
 * A message source is a component sending message on the event bus on a specific address. Message source clients are
 * {@link io.vertx.core.eventbus.MessageConsumer}.
 *
 * The _location_ or a message source service is the event bus address on which messages are sent.
 *
 * ==== Publishing a message source
 *
 * As for the other service types, publishing a message source is a 2-steps process:
 *
 * 1. create a record, using {@link io.vertx.servicediscovery.types.MessageSource}
 * 2. publish the record
 *
 * [source, $lang]
 * ----
 * {@link examples.MessageSourceExamples#example1(ServiceDiscovery)}
 * ----
 *
 * In the second record, the type of payload is also indicated. This information is optional.
 *
 * [language, java]
 * ----
 * In java, you can use {@link java.lang.Class} parameters:
 *
 * [source, $lang]
 * \----
 * {@link examples.limited.MessageSourceExamples#example1(ServiceDiscovery)}
 * \----
 * ----
 *
 * ==== Consuming a message source
 *
 * On the consumer side, you can retrieve the record and the reference, or use the
 * {@link io.vertx.servicediscovery.types.MessageSource} class to retrieve the service is one call.
 *
 * With the first approach, the code is the following:
 *
 * [source, $lang]
 * ----
 * {@link examples.MessageSourceExamples#example2(ServiceDiscovery)}
 * ----
 *
 * When, using {@link io.vertx.servicediscovery.types.MessageSource}, it becomes:
 *
 * [source, $lang]
 * ----
 * {@link examples.MessageSourceExamples#example3(ServiceDiscovery)}
 * ----
 *
 * === JDBC Data source
 *
 * Data sources represents databases or data stores. JDBC data sources are a specialization for database accessible
 * using a JDBC driver. The client of a JDBC data source service is a {@link io.vertx.ext.jdbc.JDBCClient}.
 *
 * === Publishing a JDBC service
 *
 * As for the other service types, publishing a message source is a 2-steps process:
 *
 * 1. create a record, using {@link io.vertx.servicediscovery.types.JDBCDataSource}
 * 2. publish the record
 *
 * [source, $lang]
 * ----
 * {@link examples.JDBCDataSourceExamples#example1(ServiceDiscovery)}
 * ----
 *
 * As JDBC data sources can represent a high variety of databases, and their access is often different, the record is
 * rather unstructured. The `location` is a simple JSON object that should provide the fields to access the data
 * source (JDBC url, username...). The set of field may depends on the database but also on the connection pool use
 * in front.
 *
 * === Consuming a JDBC service
 *
 * As state in the previous section, accessible data source depends on the data source itself. To build the
 * {@link io.vertx.ext.jdbc.JDBCClient}, are merged: the record location, the metadata and a json object provided by
 * the consumer:
 *
 * [source, $lang]
 * ----
 * {@link examples.JDBCDataSourceExamples#example2(ServiceDiscovery)}
 * ----
 *
 * You can also use the {@link io.vertx.ext.jdbc.JDBCClient} class to to the lookup and retrieval in one call:
 *
 * [source, $lang]
 * ----
 * {@link examples.JDBCDataSourceExamples#example3(ServiceDiscovery)}
 * ----
 * 
 * == Listening for service arrivals and departures
 * 
 * Every time a provider is published or removed, an event is published on the _vertx.discovery.announce_ address.
 * This address is configurable from the {@link io.vertx.servicediscovery.ServiceDiscoveryOptions}.
 * 
 * The received record has a `status` field indicating the new state of the record:
 * 
 * * `UP` : the service is available, you can start using it
 * * `DOWN` : the service is not available anymore, you should not use it anymore
 * * `OUT_OF_SERVICE` : the service is not running, you should not use it anymore, but it may come back later.
 *
 * == Listening for service usage
 *
 * Every time a service reference is retrieved (`bind`) or released (`release`), an event is published on the _vertx
 * .discovery.usage` address. This address is configurable from the {@link io.vertx.servicediscovery.ServiceDiscoveryOptions}.
 *
 * It lets you listen for service usage and map the service bindings.
 *
 * The received message is a {@link io.vertx.core.json.JsonObject} containing:
 *
 * * the record in the `record` field
 * * the type of event in the `type` field. It's either `bind` or `release`
 * * the id of the service discovery (either its name or the node id) in the `id` field
 *
 * This `id` is configurable from the {@link io.vertx.servicediscovery.ServiceDiscoveryOptions}. By default it's "localhost" on
 * single node configuration and the id of the node in clustered mode.
 *
 * You can disable the service usage support by setting the usage address to `null` with
 * {@link io.vertx.servicediscovery.ServiceDiscoveryOptions#setUsageAddress(java.lang.String)}.
 *
 * 
 * == Service discovery bridges
 * 
 * Bridges let import and export services from / to other discovery mechanism such as Docker, Kubernates, Consul...
 * Each bridge decides how the services are imported and exported. It does not have to be bi-directional.
 * 
 * You can provide your own bridge by implementing the {@link io.vertx.servicediscovery.spi.ServiceImporter} interface and
 * register it using
 * {@link io.vertx.servicediscovery.ServiceDiscovery#registerServiceImporter(ServiceImporter, io.vertx.core.json.JsonObject)}.
 *
 * The second parameter can provide an optional configuration for the bridge.
 *
 * When the bridge is registered the
 *
 * {@link io.vertx.servicediscovery.spi.ServiceImporter#start)}
 * method is called. It lets you configure the bridge. When the bridge is configured, ready and has imported /
 * exported the initial services, it must complete the given {@link io.vertx.core.Future}. If the bridge starts
 * method is blocking, it must uses an
 * {@link io.vertx.core.Vertx#executeBlocking(io.vertx.core.Handler, boolean, io.vertx.core.Handler)} construct, and
 * complete the given future object.
 *
 * When the service discovery is stopped, the bridge is stopped. The
 * {@link io.vertx.servicediscovery.spi.ServiceImporter#stop}
 * method is called that provides the opportunity to cleanup resources, removed imported / exported services... This
 * method must complete the given {@link io.vertx.core.Future} to notify the caller of the completion.
 * 
 * Notice than in a cluster, only one member needs to register the bridge as the records are accessible by all members.
 */
@ModuleGen(name = "vertx-service-discovery", groupPackage = "io.vertx")
@Document(fileName = "index.adoc")
package io.vertx.servicediscovery;

import io.vertx.codegen.annotations.ModuleGen;
import io.vertx.docgen.Document;
import io.vertx.servicediscovery.spi.ServiceImporter;
