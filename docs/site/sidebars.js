/**
 * Creating a sidebar enables you to:
 - create an ordered group of docs
 - render a "Next" and "Previous" button
 - automatically add a "Edit this Page" link to each doc

 More about sidebars: https://docusaurus.io/docs/sidebar
 */

// @ts-check

/** @type {import('@docusaurus/plugin-content-docs').SidebarsConfig} */
const sidebars = {
  // By default, Docusaurus generates a sidebar from the docs folder structure
  tutorialSidebar: [
    {
      type: 'doc',
      id: 'vision-overview',
      label: 'Vision & Overview',
    },
    {
      type: 'doc',
      id: 'data-model',
      label: 'Data Model',
    },
    {
      type: 'doc',
      id: 'security-compliance',
      label: 'Security & Compliance',
    },
    {
      type: 'doc',
      id: 'getting-started',
      label: 'Getting Started',
    },
     {
          type: 'doc',
          id: 'operations',
          label: 'Operations',
        },
        {
           type: 'doc',
           id: 'runbooks',
           label: 'Runbooks',
         },
         {
           type: 'doc',
           id: 'api-reference',
           label: 'API Reference',
         },
         {
           type: 'doc',
           id: 'typescript-sdks',
           label: 'TypeScript SDKs',
         },
         'intro',
         'code-samples',
        'checklists',
         'pitch-kit',
         {
           type: 'category',
           label: 'Architecture',
      items: [
        'architecture/overview',
        'architecture/microservices',
        'architecture/database-design',
        'architecture/messaging',
        'architecture/security',
      ],
    },
    {
      type: 'category',
      label: 'Services',
      items: [
        'services/auth-service',
        'services/booking-service', 
        'services/social-service',
        'services/chat-service',
        'services/gateway',
      ],
    },
    {
      type: 'category',
      label: 'Development',
      items: [
        'development/setup',
        'development/testing',
        'development/deployment',
        'development/monitoring',
      ],
    },
    {
      type: 'category',
      label: 'Frontend',
      items: [
        'frontend/overview',
        'frontend/components',
        'frontend/state-management',
        'frontend/routing',
      ],
    },
  ],
};

export default sidebars;