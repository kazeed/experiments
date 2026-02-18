# doc-signing-service-poc

Java 8 POC service that:

1. Loads DigiCert API key from AWS SSM Parameter Store key `VAULT_SIGNING__SERVICE_FR_ACCOUNTING_DIGICERT_API_KEY`.
2. Calls DigiCert List Certificates API, preferring a certificate with country `FR`.
3. Calls DigiCert Download Certificate API for the selected certificate.
4. Unzips the returned archive and extracts `.crt` (and optional private key material if included).
5. Caches the extracted certificate bundle in Redis (`COMPLIANCE_SERVICE_REDIS_CACHE`) with key `VAULT_SIGNING_SERVICE_FR_ACCOUNTING_CACHED_CERT`.
6. Signs incoming document bytes:
   - with `SHA256withRSA` when a private key is present,
   - or emits a certificate-bound SHA-256 attestation digest when only `.crt` is available.

## Build

```bash
mvn -q test
```

## Runtime configuration

Environment variables (all optional):

- `DIGICERT_API_BASE_URL` (default: `https://www.digicert.com/services/v2`)
- `DIGICERT_API_KEY_SSM_PARAM` (default: `VAULT_SIGNING__SERVICE_FR_ACCOUNTING_DIGICERT_API_KEY`)
- `COMPLIANCE_SERVICE_REDIS_CACHE` (default: `127.0.0.1`)
- `REDIS_PORT` (default: `6379`)
- `REDIS_CERT_CACHE_KEY` (default: `VAULT_SIGNING_SERVICE_FR_ACCOUNTING_CACHED_CERT`)
- `REDIS_CERT_CACHE_TTL_SECONDS` (default: `3600`)

## Notes

- This is an encapsulated POC with minimal dependencies and explicit logging around each major operation.
- A true digital signature requires private key material. If DigiCert's ZIP only contains `.crt`, this POC returns a certificate-bound digest as a traceable fallback artifact.
