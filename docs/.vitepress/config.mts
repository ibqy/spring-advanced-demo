import { defineConfig } from 'vitepress'

export default defineConfig({
  lang: 'zh-CN',
  title: 'Spring Advanced Demo',
  description: 'Spring Boot 4.1 + Spring Framework 7.0 高级特性教学',
  base: '/spring-advanced-demo/',
  lastUpdated: true,
  markdown: {
    config(md) {
      // Rewrite relative links to GitHub
      const defaultLink =
        md.renderer.rules.link_open ||
        ((tokens: any, idx: any, options: any, _env: any, self: any) => self.renderToken(tokens, idx, options))
      md.renderer.rules.link_open = (tokens: any, idx: any, options: any, env: any, self: any) => {
        const href = tokens[idx].attrGet('href')
        if (href && href.startsWith('../')) {
          const rel = href.replace(/^(\.\.\/)+/, '')
          const kind = /\.[A-Za-z]+$/.test(rel) ? 'blob' : 'tree'
          tokens[idx].attrSet('href', `https://github.com/ibqy/spring-advanced-demo/${kind}/main/${rel}`)
        }
        return defaultLink(tokens, idx, options, env, self)
      }
    }
  },
  themeConfig: {
    nav: [
      { text: '首页', link: '/' },
      { text: 'GitHub', link: 'https://github.com/ibqy/spring-advanced-demo' }
    ],
    sidebar: [
      {
        text: '教学文档',
        items: [
          { text: '01 · 整体架构', link: '/01-architecture' },
          { text: '02 · gRPC 自动配置', link: '/02-gRPC自动配置' },
          { text: '03 · HTTP Interface Client', link: '/03-HTTP-Interface-Client' },
          { text: '04 · Virtual Threads', link: '/04-Virtual-Threads' },
          { text: '05 · AOT & Native Image', link: '/05-AOT-Native-Image' },
          { text: '06 · 可观测性', link: '/06-可观测性' },
          { text: '07 · MVC 进阶', link: '/07-MVC进阶' },
          { text: '08 · Data JPA 进阶', link: '/08-Data-JPA进阶' },
          { text: '09 · 事件驱动', link: '/09-事件驱动' },
          { text: '10 · Actuator 扩展', link: '/10-Actuator扩展' },
          { text: '11 · Security 6 进阶', link: '/11-Security6进阶' }
        ]
      }
    ],
    socialLinks: [
      { icon: 'github', link: 'https://github.com/ibqy/spring-advanced-demo' }
    ]
  }
})
