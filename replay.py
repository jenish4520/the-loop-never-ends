import json, sys

log_path = 'C:/Users/vibho/.gemini/antigravity-ide/brain/a7fcf228-1d98-42b7-9813-c9815b1c6b31/.system_generated/logs/transcript.jsonl'
edits = []

with open(log_path, 'r', encoding='utf-8') as f:
    for line in f:
        try:
            data = json.loads(line)
            if 'tool_calls' in data:
                for tc in data['tool_calls']:
                    name = tc.get('name')
                    args = tc.get('args')
                    if name in ['multi_replace_file_content', 'replace_file_content', 'write_to_file', 'default_api:multi_replace_file_content', 'default_api:replace_file_content', 'default_api:write_to_file']:
                        if isinstance(args, str):
                            args = json.loads(args, strict=False)
                        
                        target_file = args.get('TargetFile', '')
                        if 'control_alt_defeat' in target_file:
                            edits.append((name, args))
        except Exception as e:
            pass

print(f'Found {len(edits)} edits.')
for name, args in edits:
    target_file = args.get('TargetFile').strip('\"\'')
    print(f'Applying {name} to {target_file}')
    try:
        with open(target_file, 'r', encoding='utf-8') as tf:
            content = tf.read()
    except FileNotFoundError:
        content = ''
    
    if name.endswith('write_to_file'):
        content = args.get('CodeContent', '')
    else:
        chunks = args.get('ReplacementChunks', []) if 'multi' in name else [args]
        if isinstance(chunks, str):
            chunks = json.loads(chunks, strict=False)
        # sort chunks by StartLine descending to avoid shifting offsets
        chunks = sorted(chunks, key=lambda x: x.get('StartLine', 0), reverse=True)
        lines = content.split('\n')
        for chunk in chunks:
            start = chunk.get('StartLine', 1) - 1
            end = chunk.get('EndLine', 1)
            replacement = chunk.get('ReplacementContent', '')
            lines = lines[:start] + replacement.split('\n') + lines[end:]
        content = '\n'.join(lines)
    
    with open(target_file, 'w', encoding='utf-8') as tf:
        tf.write(content)

print('Done applying edits.')
