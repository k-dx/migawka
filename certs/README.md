### Create the CA (Certificate Authority)

#### Private key of the CA

```sh
openssl genpkey -algorithm rsa -pkeyopt rsa_keygen_bits:4096 -aes256 -out CA.key.pem
```

#### Root certificate `CN_CA.crt`:

```sh

openssl req -new -x509 -key CA.key.pem -out CN_CA.crt -days 3650 -config ca_config.cnf

# Take a look at what we created
openssl x509 -in CN_CA.crt -noout -text
```
---


### Create private key for the server

We do not encrypt it with a password since the server will have to load it every time it starts. If it required entering password each startup, automatic restarts would be impossible.

```sh
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:4096 -out migawka_server.key.pem
```

> [!CAUTION]
> The provided config files generate certificate for local IP `192.168.5.158`. For production, they should point to a domain name where the server is hosted.

### Certificate Signing Request (CSR)


```sh
openssl req -new -key migawka_server.key.pem -out migawka_server.csr -config migawka_server.cnf

# Take a look at what we created
openssl req -in migawka_server.csr -noout -text
```

### Sign the CSR as the CA

```sh
mkdir newcerts # signed cert will be saved here
touch db.txt   # database file needed to sign

openssl ca \
  -config sign_config.cnf \
  -in migawka_server.csr \
  -out migawka_server.crt
```

### Use the certificates

For the server: provide private key `migawka_server.key.pem` and certificate `migawka_server.crt`

For the client: place `CN_CA.crt` into `@raw/cn_ca.crt`.