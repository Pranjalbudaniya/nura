-- Nura Messaging App: Supabase Database Schema & Policies
-- Run this in your Supabase Dashboard -> SQL Editor (https://supabase.com/dashboard/project/wujdvpfkodasxyhlttpo/sql)

-- 1. Ensure authenticated users can find and connect with each other
DROP POLICY IF EXISTS "Public profiles are viewable by everyone" ON public.profiles;
DROP POLICY IF EXISTS "Profiles are viewable by authenticated users" ON public.profiles;
CREATE POLICY "Profiles are viewable by authenticated users"
ON public.profiles FOR SELECT
TO authenticated
USING (true);

-- 2. Create the temporary message relay table
CREATE TABLE IF NOT EXISTS public.messages_relay (
    message_id TEXT PRIMARY KEY,
    conversation_id TEXT NOT NULL,
    sender_id UUID NOT NULL,
    receiver_id UUID NOT NULL,
    content TEXT NOT NULL,
    message_type TEXT NOT NULL DEFAULT 'text',
    delivery_status TEXT NOT NULL DEFAULT 'SENT_TO_SERVER',
    created_at BIGINT NOT NULL
);

-- 3. Indexes for fast relay lookups and deletions
CREATE INDEX IF NOT EXISTS idx_messages_relay_receiver ON public.messages_relay(receiver_id);
CREATE INDEX IF NOT EXISTS idx_messages_relay_sender ON public.messages_relay(sender_id);
CREATE INDEX IF NOT EXISTS idx_messages_relay_conv ON public.messages_relay(conversation_id);

-- 4. Enable Row Level Security (RLS)
ALTER TABLE public.messages_relay ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can insert messages to relay" ON public.messages_relay;
CREATE POLICY "Users can insert messages to relay"
ON public.messages_relay FOR INSERT
TO authenticated
WITH CHECK (true);

DROP POLICY IF EXISTS "Users can view messages sent to them" ON public.messages_relay;
CREATE POLICY "Users can view messages sent to them"
ON public.messages_relay FOR SELECT
TO authenticated
USING (auth.uid() = receiver_id);

DROP POLICY IF EXISTS "Receivers can delete messages upon receipt" ON public.messages_relay;
CREATE POLICY "Receivers can delete messages upon receipt"
ON public.messages_relay FOR DELETE
TO authenticated
USING (auth.uid() = receiver_id);

-- 5. Enable Realtime on messages_relay table
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables 
        WHERE pubname = 'supabase_realtime' 
        AND schemaname = 'public' 
        AND tablename = 'messages_relay'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.messages_relay;
    END IF;
END $$;
