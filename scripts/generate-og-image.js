const sharp = require('sharp');
const fs = require('fs');
const path = require('path');

const escapeXml = (value) =>
  String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');

const generateOGImage = async (outputPath, title, subtitle, bgColor, accentColor) => {
  const safeTitle = escapeXml(title);
  const safeSubtitle = escapeXml(subtitle);
  const svg = `
    <svg width="1200" height="630" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <linearGradient id="grad" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" style="stop-color:${bgColor};stop-opacity:1" />
          <stop offset="100%" style="stop-color:${accentColor};stop-opacity:1" />
        </linearGradient>
      </defs>
      <rect width="1200" height="630" fill="url(#grad)"/>
      <text x="600" y="250" font-family="Inter, sans-serif" font-size="72" font-weight="bold" fill="white" text-anchor="middle">${safeTitle}</text>
      <text x="600" y="350" font-family="Inter, sans-serif" font-size="36" fill="white" text-anchor="middle" opacity="0.9">${safeSubtitle}</text>
    </svg>
  `;

  await fs.promises.mkdir(path.dirname(outputPath), { recursive: true });
  await sharp(Buffer.from(svg)).png().toFile(outputPath);
  console.log(`Generated: ${outputPath}`);
};

(async () => {
  // Medical
  await generateOGImage(
    path.join(__dirname, '../kin-frontend-medical/public/og-image.png'),
    'KIN Medical',
    'El Sistema Operativo de la Atención Médica',
    '#0EA5E9',
    '#10B981'
  );

  // Empresas
  await generateOGImage(
    path.join(__dirname, '../kin-frontend/public/og-image.png'),
    'KIN',
    'Knowledge, Innovation & Navigation',
    '#4F46E5',
    '#7C3AED'
  );
})();
