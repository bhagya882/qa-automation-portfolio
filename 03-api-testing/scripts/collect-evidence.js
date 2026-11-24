// Copies Newman + REST Assured results into docs/evidence and redacts any JWT (access/refresh tokens)
// so no credentials end up in the repository. Usage: npm run evidence
const fs = require('fs');
const path = require('path');

const out = path.join(__dirname, '..', 'docs', 'evidence');
const sources = ['target/newman', 'target/newman-defects'].map(d => path.join(__dirname, '..', d));
const JWT = /eyJ[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{20,}/g;

fs.mkdirSync(out, { recursive: true });
let copied = 0;
for (const dir of sources) {
  if (!fs.existsSync(dir)) continue;
  for (const f of fs.readdirSync(dir)) {
    const text = fs.readFileSync(path.join(dir, f), 'utf8').replace(JWT, '<redacted-jwt>');
    fs.writeFileSync(path.join(out, f), text);
    copied++;
  }
}
console.log(`Copied ${copied} file(s) to ${out} (JWTs redacted)`);
