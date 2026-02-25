const fs = require('fs');
const path = require('path');
const { fileURLToPath } = require('url');

const files = [
    'src/pages/Dashboard.jsx',
    'src/pages/LogsView.jsx',
    'src/pages/BlacklistView.jsx',
    'src/pages/PipelineView.jsx'
];

const replacements = [
    { regex: /\btext-white\b/g, replacement: 'text-slate-900 dark:text-white' },
    { regex: /\bbg-white\/5\b/g, replacement: 'bg-slate-200 dark:bg-white/5' },
    { regex: /\bbg-white\/\[0\.02\]\b/g, replacement: 'bg-slate-100 dark:bg-white/[0.02]' },
    { regex: /\bbg-white\/\[0\.03\]\b/g, replacement: 'bg-slate-100 dark:bg-white/[0.03]' },
    { regex: /\bborder-white\/5\b/g, replacement: 'border-slate-200 dark:border-white/5' },
    { regex: /\bborder-white\/10\b/g, replacement: 'border-slate-300 dark:border-white/10' },
    { regex: /\btext-slate-200\b/g, replacement: 'text-slate-800 dark:text-slate-200' },
    { regex: /\btext-slate-300\b/g, replacement: 'text-slate-700 dark:text-slate-300' },
    { regex: /\btext-slate-400\b/g, replacement: 'text-slate-600 dark:text-slate-400' },
    { regex: /\btext-slate-500\b/g, replacement: 'text-slate-500 dark:text-slate-500' },
    { regex: /\btext-slate-600\b/g, replacement: 'text-slate-400 dark:text-slate-600' },
    { regex: /\bbg-purple-600\/10\b/g, replacement: 'bg-purple-100 dark:bg-purple-600/10' },
    { regex: /\bborder-purple-500\/20\b/g, replacement: 'border-purple-200 dark:border-purple-500/20' }
];

files.forEach(file => {
    const filePath = path.join(__dirname, file);
    if (fs.existsSync(filePath)) {
        let content = fs.readFileSync(filePath, 'utf8');
        replacements.forEach(({ regex, replacement }) => {
            content = content.replace(regex, replacement);
        });
        fs.writeFileSync(filePath, content);
        console.log(`Updated ${file}`);
    } else {
        console.error(`File not found: ${file}`);
    }
});
