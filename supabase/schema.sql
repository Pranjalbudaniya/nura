-- ==============================================================================
-- Nura Messaging App: Complete Supabase Database Schema & RLS Policies
-- ==============================================================================
-- Run this in your Supabase Project Dashboard -> SQL Editor:
-- https://supabase.com/dashboard/project/_/sql
-- (All statements are idempotent and safe to run multiple times)
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. Profiles Table & Columns
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    display_name TEXT NOT NULL DEFAULT '',
    username TEXT NOT NULL DEFAULT '',
    avatar_url TEXT,
    about TEXT DEFAULT 'HI there i''m using nura',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Ensure optional columns exist if table was already created earlier
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS about TEXT DEFAULT 'HI there i''m using nura';
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS avatar_url TEXT;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS display_name TEXT DEFAULT '';
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS username TEXT DEFAULT '';
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now());

-- Enable Row Level Security (RLS) on profiles
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

-- Profiles Policies
DROP POLICY IF EXISTS "Public profiles are viewable by everyone" ON public.profiles;
DROP POLICY IF EXISTS "Profiles are viewable by authenticated users" ON public.profiles;
CREATE POLICY "Profiles are viewable by authenticated users"
ON public.profiles FOR SELECT
TO authenticated
USING (true);

DROP POLICY IF EXISTS "Users can insert own profile" ON public.profiles;
CREATE POLICY "Users can insert own profile"
ON public.profiles FOR INSERT
TO authenticated
WITH CHECK (auth.uid() = id);

DROP POLICY IF EXISTS "Users can update own profile" ON public.profiles;
CREATE POLICY "Users can update own profile"
ON public.profiles FOR UPDATE
TO authenticated
USING (auth.uid() = id)
WITH CHECK (auth.uid() = id);

-- ------------------------------------------------------------------------------
-- 2. Message Relay Table (Transient queue for offline & realtime delivery)
-- ------------------------------------------------------------------------------
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

-- Indexes for fast queries and queue management
CREATE INDEX IF NOT EXISTS idx_messages_relay_receiver ON public.messages_relay(receiver_id);
CREATE INDEX IF NOT EXISTS idx_messages_relay_sender ON public.messages_relay(sender_id);
CREATE INDEX IF NOT EXISTS idx_messages_relay_conv ON public.messages_relay(conversation_id);

-- Enable Row Level Security on messages_relay
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

-- ------------------------------------------------------------------------------
-- 3. Enable Realtime on messages_relay
-- ------------------------------------------------------------------------------
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

-- ------------------------------------------------------------------------------
-- 4. Storage Bucket for Avatars & Pictures
-- ------------------------------------------------------------------------------
INSERT INTO storage.buckets (id, name, public)
VALUES ('avatars', 'avatars', true)
ON CONFLICT (id) DO NOTHING;

-- Storage RLS Policies
DROP POLICY IF EXISTS "Public Avatar Access" ON storage.objects;
CREATE POLICY "Public Avatar Access"
ON storage.objects FOR SELECT
TO public
USING (bucket_id = 'avatars');

DROP POLICY IF EXISTS "Authenticated users can upload avatars" ON storage.objects;
CREATE POLICY "Authenticated users can upload avatars"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (bucket_id = 'avatars');

DROP POLICY IF EXISTS "Users can update own avatar" ON storage.objects;
CREATE POLICY "Users can update own avatar"
ON storage.objects FOR UPDATE
TO authenticated
USING (bucket_id = 'avatars');
