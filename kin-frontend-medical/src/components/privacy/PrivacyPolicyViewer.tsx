'use client';

import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

interface PrivacyPolicyViewerProps {
  content: string;
  className?: string;
}

export function PrivacyPolicyViewer({ content, className = '' }: PrivacyPolicyViewerProps) {
  return (
    <div className={`prose prose-lg max-w-none prose-medical ${className}`}>
      <ReactMarkdown remarkPlugins={[remarkGfm]} components={{
        a: ({ href, children, ...props }) => (
          <a
            href={href}
            target={href.startsWith('http') ? '_blank' : undefined}
            rel={href.startsWith('http') ? 'noopener noreferrer' : undefined}
            className="text-medical-600 hover:text-medical-700 underline hover:no-underline transition-colors"
            {...props}
          >
            {children}
          </a>
        ),
        h1: (props) => <h1 className="text-3xl font-bold text-neutral-900 mb-4 mt-8 first:mt-0">{props.children}</h1>,
        h2: (props) => <h2 className="text-2xl font-bold text-neutral-900 mb-3 mt-8">{props.children}</h2>,
        h3: (props) => <h3 className="text-xl font-bold text-neutral-900 mb-2 mt-6">{props.children}</h3>,
        p: (props) => <p className="text-neutral-700 mb-4 leading-relaxed">{props.children}</p>,
        ul: (props) => <ul className="list-disc pl-6 mb-4 space-y-1">{props.children}</ul>,
        ol: (props) => <ol className="list-decimal pl-6 mb-4 space-y-1">{props.children}</ol>,
        li: (props) => <li className="text-neutral-700 leading-relaxed">{props.children}</li>,
        strong: (props) => <strong className="font-semibold text-neutral-900">{props.children}</strong>,
        em: (props) => <em className="italic text-neutral-700">{props.children}</em>,
        code: (props) => <code className="bg-neutral-100 text-medical-700 px-1.5 py-0.5 rounded text-sm font-mono">{props.children}</code>,
        pre: (props) => <pre className="bg-neutral-900 text-neutral-100 p-4 rounded-lg overflow-x-auto mb-4"><code>{props.children}</code></pre>,
        blockquote: (props) => (
          <blockquote className="border-l-4 border-medical-500 pl-4 italic text-neutral-600 my-4">
            {props.children}
          </blockquote>
        ),
        hr: () => <hr className="border-neutral-200 my-8" />,
        table: (props) => (
          <div className="overflow-x-auto mb-4">
            <table className="min-w-full divide-y divide-neutral-200">
              {props.children}
            </table>
          </div>
        ),
        thead: (props) => (
          <thead className="bg-neutral-50">
            <tr>{props.children}</tr>
          </thead>
        ),
        tbody: (props) => <tbody className="divide-y divide-neutral-200">{props.children}</tbody>,
        th: (props) => (
          <th className="px-4 py-3 text-left text-xs font-semibold text-neutral-700 uppercase tracking-wider bg-neutral-50 border-b border-neutral-200">
            {props.children}
          </th>
        ),
        td: (props) => (
          <td className="px-4 py-3 text-sm text-neutral-700 border-b border-neutral-200">{props.children}</td>
        ),
      }}>
        {content}
      </ReactMarkdown>
    </div>
  );
}