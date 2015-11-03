#!/bin/bash
currentdir=$(dirname $0)
echo $currentdir
mkdir ${currentdir}/sslcerts
cd ${currentdir}/sslcerts

echo '
[ v3_client ]
basicConstraints=CA:FALSE
authorityKeyIdentifier=keyid,issuer
subjectKeyIdentifier=hash
extendedKeyUsage=clientAuth
keyUsage=digitalSignature, nonRepudiation, keyEncipherment

[ v3_ca_only ]
subjectKeyIdentifier=hash
authorityKeyIdentifier=keyid:always,issuer:always
basicConstraints=CA:TRUE
keyUsage=keyCertSign

[ v3_intermediate ]
subjectKeyIdentifier=hash
authorityKeyIdentifier=keyid:always,issuer:always
basicConstraints=CA:TRUE
subjectKeyIdentifier=hash
keyUsage=keyCertSign

[ v3_server ]
subjectKeyIdentifier=hash
authorityKeyIdentifier=keyid:always,issuer:always
basicConstraints=CA:FALSE
subjectKeyIdentifier=hash
extendedKeyUsage=serverAuth
subjectAltName = @alt_names

[alt_names]
DNS.1 = aws.localhost.com
' > openssl.cnf

if [ "x${JAVA_HOME}" == "x" ] ; then
    echo "You must set the environment variable JAVA_HOME to the jdk"
    exit 1;
else
    if [ -f $JAVA_HOME/bin/keytool ];
    then
       : # do nothing
    else
        echo  "$JAVA_HOME/bin/keytool not found"
        exit;
    fi
fi

opensslcnf="openssl.cnf"


echo "====================="
echo "Certing Test Certs fro 1825 days (see http://sourceforge.net/projects/portecle/) for a gui for viewing certs"
echo "====================="



echo "--------------------"
echo "Creating the CA     "
echo "--------------------"

openssl req -newkey rsa:2048 -nodes -out cacerts.csr -keyout cacerts.key -days 1825 \
 -subj "/C=GB/ST=London/L=London/O=Greencheek/OU=Development/CN=Root CA/emailAddress=greencheek-root@devnull.com"

openssl x509 -req -trustout -signkey cacerts.key -days 1825 -req -in cacerts.csr \
 -out cacerts.pem -extfile ${opensslcnf} -extensions v3_ca_only

$JAVA_HOME/bin/keytool -import -keystore cacerts.jks -file cacerts.pem \
 -alias ROOT_CA -storepass password -noprompt

echo "------------------------------"
echo "Creating the Intermediate     "
echo "------------------------------"

openssl req -newkey rsa:2048 -nodes -out intermediate.csr -keyout intermediate.key \
 -days 1825 -subj "/CN=*.aws.localhost.com/OU=Development/O=Greencheek/L=London/ST=London/C=UK/emailAddress=greencheek-intermediate@devnull.com"

openssl x509 -CA cacerts.pem -CAkey cacerts.key -set_serial 02 \
 -req -in intermediate.csr -out intermediate.pem -days 1825 \
-extfile ${opensslcnf} -extensions v3_intermediate

$JAVA_HOME/bin/keytool -import -keystore intermediate.truststore \
 -file cacerts.pem -alias ROOT_CA -storepass password -noprompt

echo "------------------------"
echo "Creating the Server     "
echo "------------------------"

openssl req -newkey rsa:2048 -nodes -out server.csr -keyout server.key \
 -days 1825 -subj "/CN=*.aws.localhost.com/OU=Development/O=Greencheek/L=London/ST=London/C=UK/emailAddress=greencheek-server@devnull.com"

 
openssl x509 -CA intermediate.pem -CAkey intermediate.key -set_serial 02 \
 -req -in server.csr -out server.pem -days 1825 \
-extfile ${opensslcnf} -extensions v3_server

$JAVA_HOME/bin/keytool -import -keystore server.truststore \
 -file cacerts.pem -alias ROOT_CA -storepass password -noprompt

$JAVA_HOME/bin/keytool -import -keystore server.truststore \
 -file intermediate.pem -alias INTERMEDIATE_CA -storepass password -noprompt

openssl pkcs12 -export -name server -in server.pem -inkey server.key \
 -certfile intermediate.pem -out server.p12 -passout pass:password


# Convert PKCS12 keystore into a JKS keystore
$JAVA_HOME/bin/keytool -importkeystore -destkeystore server.jks -deststorepass password -srckeystore server.p12 -srcstoretype pkcs12 -alias server -srcstorepass password

echo "--------------------"
echo "Creating the Client, Signed by the Server "
echo "--------------------"

openssl req -newkey rsa:2048 -nodes -out client.csr -keyout client.key \
 -days 1825 -subj "/CN=Client/OU=Development/O=Greencheek/L=London/ST=London/C=UK/emailAddress=greencheek-client@devnull.com"

openssl x509 -CA intermediate.pem -CAkey intermediate.key \
 -set_serial 02 -req -in client.csr -out client.pem -days 1825 \
-extfile ${opensslcnf} -extensions v3_client

openssl pkcs12 -export -in client.pem -inkey client.key \
 -certfile intermediate.pem -out client.p12 -passout pass:password
 
echo "--------------------"
echo "Creating the Client TrustStore containing the Server Public Cert (and jdk cacerts) "
echo "--------------------"

cp $JAVA_HOME/jre/lib/security/cacerts client.truststore

$JAVA_HOME/bin/keytool -import -keystore client.truststore \
 -file intermediate.pem -alias INTERMEDIATE_CA -storepass changeit -noprompt

$JAVA_HOME/bin/keytool -import -keystore client.truststore \
 -file cacerts.pem -alias ROOT_CA -storepass changeit -noprompt

$JAVA_HOME/bin/keytool -import -keystore client.truststore \
 -file server.pem -alias SERVER_CA -storepass changeit -noprompt