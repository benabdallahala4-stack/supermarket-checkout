import js from '@eslint/js';
import { defineConfig, globalIgnores } from 'eslint/config';
import tseslint from 'typescript-eslint';
import angular from 'angular-eslint';

export default defineConfig([
  globalIgnores(['src/app/generated/**', 'dist/**', 'coverage/**', '.angular/**']),
  {
    files: ['**/*.ts'],
    extends: [
      js.configs.recommended,
      ...tseslint.configs.recommended,
      ...angular.configs.tsRecommended,
    ],
    processor: angular.processInlineTemplates,
    rules: {
      '@angular-eslint/prefer-on-push-component-change-detection': 'error',
    },
  },
  {
    files: ['**/*.html'],
    extends: [...angular.configs.templateRecommended, ...angular.configs.templateAccessibility],
  },
]);
