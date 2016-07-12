# Flow

## init
- read config+args
- start akka

## decide resource allocation
- query freamon
- no similar runs available?
  - use initial resource allocation
- similar runs available?
  - build model (-> JBLAS, Ilya)
  - calculate scale-out
  - apply constraints

## run job
- scan and store log (stdout + stderr)
- make freamon start recording
- ? catch errors and notify+shutdown
- make freamon stop recording


# Interaction

## input

### args
- jar, args
- resource allocation: initial, constraints
- preferred runtime

### config
- hadoop/yarn
- flink
- own akka
- log output dir
- ? cluster hardware spec
- freamon host, port

### internal application.conf
- own system name, actor name, port
- freamon system name, actor name, default port
  - import freamon's application.conf

## communication with Freamon
- via akka
- get jobs matching jar+args from db
- start recording for job with job id
- stop recording for job with job id

## communication with Flink
- via shell/log
- execute flink command
  - jar, args
  - resource allocation
- read job start, end
- read app id

## communication with Yarn?
- via yarn api (akka?) -> freamon-yarn-client?
- do we need to interact with hadoop for running flink?
