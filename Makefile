EXE_NAME	=	build/libs/koak-1.0-SNAPSHOT.jar


all:		compile generateScript

generateScript:
		@echo -e "#!/bin/bash\nif [[ \$$# -eq 0 ]]; then\njava -jar build/libs/koak-1.0-SNAPSHOT.jar\nelse\njava -jar build/libs/koak-1.0-SNAPSHOT.jar \$$1\nfi" > ./koak
		@chmod +x koak

compile:	$(EXE_NAME)

$(EXE_NAME):
		gradle build

clean:
		rm -rf koak
		gradle clean

re: 		clean all
