import type {
  ICredentialTestRequest,
  ICredentialType,
  INodeProperties,
} from 'n8n-workflow';

export class OneCaiApi implements ICredentialType {
  name = 'oneCaiApi';

  displayName = '1C AI Stack API';

  documentationUrl = 'https://github.com/DmitrL-dev/1cai-public';

  properties: INodeProperties[] = [
    {
      displayName: 'Base URL',
      name: 'baseUrl',
      type: 'string',
      default: 'http://localhost:8080',
      placeholder: 'https://your-domain/api',
      description:
        'Базовый URL API 1C AI Stack. Пример: http://localhost:8080 или https://prod.yourdomain.com',
      required: true,
    },
    {
      displayName: 'API Key',
      name: 'apiKey',
      type: 'string',
      typeOptions: {
        password: true,
      },
      default: '',
      description:
        'Bearer токен или API ключ для доступа к защищённым эндпоинтам. Можно оставить пустым для локальной разработки без авторизации.',
    },
    {
      displayName: 'Reject Unauthorized SSL',
      name: 'ignoreSslIssues',
      type: 'boolean',
      default: false,
      description:
        'Отключить проверку SSL сертификата (используйте только в dev окружении).',
    },
  ];

  credentialTest = {
    request: {
      method: 'GET',
      url: '/health',
    } satisfies ICredentialTestRequest,
  };
}

