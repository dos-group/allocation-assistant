# allocation-assistant
resource allocation for recurring parallel dataflow jobs

## Compiling
install the `dev` branch of Freamon:

	git clone <freamon url>
	cd freamon
	git checkout dev
	mvn install

compile allocation-assistant:

	cd ..       # where you cloned allocation-assistant
	mvn package

## Running
1. setup+start freamon (see freamon readme)
1. create your own config based on `doc/cluster.conf`
1. `./allocation-assistent --help`
1. find out the right args for your use case
1. `./allocation-assistent <your args>`
