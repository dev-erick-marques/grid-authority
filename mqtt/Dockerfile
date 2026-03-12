FROM hivemq/hivemq-ce:2024.7

RUN keytool -genkeypair \
      -keyalg EC \
      -groupname secp256r1 \
      -alias hivemq \
      -dname "CN=hivemq, O=GridAuthority, C=BR" \
      -keystore /opt/hivemq/conf/keystore.jks \
      -storepass gridauth \
      -keypass gridauth \
      -validity 3650

COPY config/config.xml /opt/hivemq/conf/config.xml
