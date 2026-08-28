# ─────────────────────────────────────────────────────────────────────────────
# Etapa 1: build
#
# As dependencias sao baixadas antes de copiar o codigo, para que uma mudanca
# em src nao invalide o cache do Maven e refaca o download a cada build.
# ─────────────────────────────────────────────────────────────────────────────
FROM maven:3-eclipse-temurin-26 AS build

WORKDIR /build

COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src

# -DskipTests, no plural. O Dockerfile anterior escrevia "-DskipTest", uma
# propriedade que o Maven ignora; o -Dmaven.test.skip=true que vinha junto e que
# de fato pulava os testes, entao o erro de digitacao nunca apareceu.
RUN mvn -B -ntp clean package -DskipTests

# Abre o jar em camadas. As dependencias mudam pouco e o codigo da aplicacao
# muda a cada commit: separados, um deploy novo reenvia poucos megabytes em vez
# do jar inteiro de 70 MB.
#
# O layout do Spring Boot 4 e diferente do antigo layertools: o jar fino sai em
# application/ e espera encontrar lib/ ao lado dele. A normalizacao abaixo monta
# exatamente esse arranjo, e resolve tambem o caso de snapshot-dependencies vir
# vazio (um COPY direto de um diretorio inexistente quebraria o build).
RUN java -Djarmode=tools -jar target/*.jar extract --layers --destination extraido \
 && mkdir -p imagem/lib \
 && cp extraido/application/*.jar imagem/aplicacao.jar \
 && cp -r extraido/dependencies/lib/. imagem/lib/ \
 && if [ -d extraido/snapshot-dependencies/lib ]; then \
        cp -r extraido/snapshot-dependencies/lib/. imagem/lib/; \
    fi

# ─────────────────────────────────────────────────────────────────────────────
# Etapa 2: execucao
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre

# curl e usado pelo HEALTHCHECK abaixo.
RUN apt-get update \
 && apt-get install --no-install-recommends -y curl \
 && rm -rf /var/lib/apt/lists/*

# Usuario sem privilegios: a imagem anterior rodava como root, entao uma falha
# na aplicacao daria acesso administrativo ao container.
RUN groupadd --system cantina \
 && useradd --system --gid cantina --create-home --home-dir /app cantina

WORKDIR /app

# lib/ primeiro (camada grande e estavel), aplicacao.jar depois (camada pequena
# que muda a cada commit): so a segunda e reenviada num deploy comum.
COPY --from=build --chown=cantina:cantina /build/imagem/lib ./lib
COPY --from=build --chown=cantina:cantina /build/imagem/aplicacao.jar ./aplicacao.jar

USER cantina

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC" \
    SERVER_PORT=8080 \
    TZ=America/Sao_Paulo

# O orquestrador precisa saber se o container esta atendendo, nao apenas se o
# processo existe. start-period cobre a subida do Spring e as migrations.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS "http://localhost:${SERVER_PORT}/actuator/health/readiness" || exit 1

# exec para o java virar PID 1 e receber o SIGTERM do orquestrador, permitindo
# o desligamento gracioso configurado em server.shutdown=graceful.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/aplicacao.jar"]
